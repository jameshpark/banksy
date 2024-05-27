package org.jameshpark.banksy.loader

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import org.jameshpark.banksy.database.Dao
import org.jameshpark.banksy.models.Feed
import org.jameshpark.banksy.models.Transaction
import org.jameshpark.banksy.utils.chunked

class DefaultLoader(private val dao: Dao) : Loader {

    override suspend fun saveTransactions(feed: Feed, transactions: Flow<Transaction>) {
        transactions.chunked(500).collect { chunk ->
            dao.saveTransactions(chunk)
            logger.info { "Saved ${chunk.size} transactions" }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}
