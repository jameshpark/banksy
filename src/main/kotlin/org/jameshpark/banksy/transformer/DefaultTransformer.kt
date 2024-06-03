package org.jameshpark.banksy.transformer

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.jameshpark.banksy.models.Category
import org.jameshpark.banksy.models.Extracted
import org.jameshpark.banksy.models.Transaction
import java.util.concurrent.atomic.AtomicInteger

class DefaultTransformer : Transformer {

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun transform(rows: Flow<Extracted>, sourceName: String?): Flow<Transaction> {
        val counter = AtomicInteger(0)

        return rows.flatMapMerge { row ->
            flow {
                val transaction = row.toTransaction()

                if (transaction != null) {
                    if (transaction.category == Category.UNCATEGORIZED) {
                        logger.info { "UNCATEGORIZED transaction for merchant '${transaction.description}'" }
                    }

                    emit(transaction)

                    if (counter.incrementAndGet() % 100 == 0) {
                        logger.info { "Processed ${counter.get()} transactions ${sourceName?.let { "from $it" } ?: ""}" }
                    }
                }
            }
        }.onCompletion {
            logger.info { "Processed a total of ${counter.get()} transactions ${sourceName?.let { "from $it" } ?: ""}" }
        }.filterNotNull()
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}
