package org.jameshpark.banksy.loader

import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jameshpark.banksy.database.Dao
import org.jameshpark.banksy.models.Feed
import org.jameshpark.banksy.models.Transaction
import java.io.File
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

class DefaultLoader(private val dao: Dao, private val writer: CsvWriter = csvWriter()) : Loader {
    override suspend fun saveTransactions(feed: Feed, transactions: Flow<Transaction>) {
        dao.saveTransactions(transactions)
    }

    override suspend fun exportToCsv(feed: Feed, filePath: String, includeHeader: Boolean) {
        logger.info { "Exporting to $filePath" }
        val output = File(filePath)

        // create new file
        val fileCreation = withContext(Dispatchers.IO) {
            launch {
                output.createNewFile()
                if (includeHeader) {
                    output.writeText("date,description,amount,category,critical,type,originHash\n")
                }
            }
        }

        val exportBookmark = dao.getPreviousBookmarkByName(feed.getBookmarkName()) ?: LocalDate.EPOCH
        val transactions = dao.getTransactionsSinceDate(exportBookmark)

        val counter = AtomicInteger(0)
        fileCreation.join()
        writer.openAsync(output, append = true) {
            transactions.collect {
                writeRow(it.toCsvRow())
                if (counter.incrementAndGet() % 10 == 0) {
                    logger.info { "Exported ${counter.get()} transactions to $filePath" }
                }
            }
        }
        logger.info { "Exported ${counter.get()} transactions to $filePath" }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}

