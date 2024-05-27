package org.jameshpark.banksy.transformer

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.jameshpark.banksy.models.Category
import org.jameshpark.banksy.models.Transaction
import org.jameshpark.banksy.models.toTransaction
import java.util.concurrent.atomic.AtomicInteger

class DefaultTransformer : Transformer {

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun transform(rows: Flow<Map<String, String>>, sourceName: String?): Flow<Transaction> {
        val counter = AtomicInteger(0)

        return rows.flatMapMerge { row ->
            flow {
                val mapper = headersToMapper[row.keys]

                if (mapper != null) {
                    val transaction = row.toTransaction(mapper)
                    if (transaction.category == Category.UNCATEGORIZED) {
                        logger.info { "UNCATEGORIZED transaction for merchant '${transaction.description}'" }
                    }

                    emit(transaction)

                    if (counter.incrementAndGet() % 100 == 0) {
                        logger.info { "Parsed ${counter.get()} transactions ${sourceName?.let { "from $it" } ?: ""}" }
                    }
                } else {
                    logger.warn { "No mapper for headers '${row.keys}', skipping row '${row.values}'" }
                    emit(null)
                }
            }
        }.onCompletion {
            logger.info { "Parsed a total of ${counter.get()} transactions ${sourceName?.let { "from $it" } ?: ""}" }
        }.filterNotNull()
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}
