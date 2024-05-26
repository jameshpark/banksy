package org.jameshpark.banksy.loader

import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import org.jameshpark.banksy.database.Dao
import org.jameshpark.banksy.models.Feed
import org.jameshpark.banksy.models.Transaction
import org.jameshpark.banksy.utils.chunked

class DefaultLoader(private val dao: Dao, private val writer: CsvWriter = csvWriter()) : Loader {
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

