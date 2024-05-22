package org.jameshpark.banksy.transformer

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.jameshpark.banksy.models.Category
import org.jameshpark.banksy.models.Transaction
import org.jameshpark.banksy.models.TransactionType
import org.jameshpark.banksy.models.toTransaction
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger

class DefaultTransformer : Transformer {
    override suspend fun transform(rows: Flow<Map<String, String>>): Flow<Transaction> {
        return parseTransactions(rows)
    }

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
    private fun parseTransactions(rows: Flow<Map<String, String>>): Flow<Transaction> {
        val counter = AtomicInteger(0)
        return rows.flatMapMerge { row ->
            flow {
                val mapper = headersToMapper[row.keys]

                if (mapper != null) {
                    emit(row.toTransaction(mapper))
                    if (counter.incrementAndGet() % 100 == 0) {
                        logger.info { "Parsed ${counter.get()} transactions" }
                    }
                } else {
                    logger.warn { "No mapper for headers '${row.keys}', skipping row '${row.values}'" }
                    emit(null)
                }
            }
        }.onCompletion {
            logger.info { "Parsed a total of ${counter.get()} transactions" }
        }.filterNotNull()
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}