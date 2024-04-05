package org.jameshpark.banksy

import kotlinx.coroutines.flow.Flow
import org.jameshpark.banksy.models.Category
import org.jameshpark.banksy.models.TransactionType
import org.jameshpark.org.jameshpark.banksy.models.Transaction
import java.math.BigDecimal

object Transformer {
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
}