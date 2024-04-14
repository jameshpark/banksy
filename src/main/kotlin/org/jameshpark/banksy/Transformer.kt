package org.jameshpark.banksy

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import org.jameshpark.banksy.models.Category
import org.jameshpark.banksy.models.TransactionType
import org.jameshpark.banksy.models.Transaction
import org.jameshpark.banksy.models.toTransaction
import java.math.BigDecimal

object Transformer {
    fun transform(rows: Flow<Map<String, String>>): Flow<Transaction> = parseTransactions(rows)

    suspend fun spendingByCategory(transactions: Flow<Transaction>): Map<Category, BigDecimal> {
        val spendingByCategory = mutableMapOf<Category, BigDecimal>()
        transactions.collect { transaction ->
            val amount = when (transaction.type) {
                TransactionType.DEBIT -> transaction.amount
                TransactionType.CREDIT -> -transaction.amount
            }

            val sum = spendingByCategory[transaction.category]?.let {
                it + amount
            } ?: amount

            spendingByCategory[transaction.category] = sum
        }

        return spendingByCategory.toMap()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun parseTransactions(rows: Flow<Map<String, String>>): Flow<Transaction> = rows.flatMapMerge { row ->
        flow {
            val mapper = headersToMapper[row.keys]

            if (mapper != null) {
                emit(row.toTransaction(mapper))
            } else {
                println("No mapper for headers '${row.keys}', skipping row '${row.values}'")
                emit(null)
            }
        }
    }.filterNotNull()
}