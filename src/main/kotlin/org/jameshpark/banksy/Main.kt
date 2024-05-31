package org.jameshpark.banksy

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jameshpark.banksy.database.Dao
import org.jameshpark.banksy.database.DefaultDatabase
import org.jameshpark.banksy.exporter.CsvExporter
import org.jameshpark.banksy.exporter.GoogleSheetsExporter
import org.jameshpark.banksy.extractor.CsvExtractor
import org.jameshpark.banksy.loader.DefaultLoader
import org.jameshpark.banksy.models.CsvFeed
import org.jameshpark.banksy.models.CsvSink
import org.jameshpark.banksy.models.GoogleSheetsSink
import org.jameshpark.banksy.transformer.DefaultTransformer
import org.jameshpark.banksy.utils.csvFeedsFromProperties
import org.jameshpark.banksy.utils.launchApp
import org.jameshpark.banksy.utils.sheetsServiceFromProperties
import java.util.Properties

private val logger = KotlinLogging.logger { }

fun main() = launchApp {
    val db = DefaultDatabase.fromProperties(properties).register()
    val dao = Dao(db)
    val csvFeeds = csvFeedsFromProperties(properties).also {
        if (it.isEmpty()) {
            logger.info { "No transaction csv files found. Quitting..." }
            return@launchApp
        }
    }
    val transactionIdBeforeLoad = dao.getLatestTransactionId()

    processFeeds(csvFeeds, dao)
    exportData(properties, dao, transactionIdBeforeLoad)
}

/**
 * Processes a list of CSV feeds asynchronously.
 *
 * Data from each CSV feed is extracted, filtered, transformed, and then loaded into a local database.
 *
 * @param csvFeeds The list of CSV feeds to process.
 * @param dao The DataAccessObject used for saving transactions and retrieving bookmarks.
 */
suspend fun processFeeds(csvFeeds: List<CsvFeed>, dao: Dao) {
    val extractor = CsvExtractor(dao)
    val transformer = DefaultTransformer()
    val loader = DefaultLoader(dao)

    coroutineScope {
        csvFeeds.map { feed ->
            launch {
                val rows = extractor.extract(feed)
                val transactions = transformer.transform(rows, feed.file.name)
                loader.saveTransactions(feed, transactions)
            }
        }
    }
}

/**
 * Export data using the provided properties and DAO.
 *
 * This method exports data to both CSV and Google Sheets in parallel.
 *
 * @param properties The properties containing the necessary configuration for exporting data.
 * @param dao The DAO object that handles database operations.
 * @param transactionIdBeforeLoad The ID of the latest transaction before the data is loaded.
 */
suspend fun exportData(properties: Properties, dao: Dao, transactionIdBeforeLoad: Int) {
    val csvExporter = CsvExporter(dao)
    val googleSheetsExporter = GoogleSheetsExporter(dao, sheetsServiceFromProperties(properties))

    coroutineScope {
        launch {
            csvExporter.export(
                CsvSink.fromProperties(properties),
                transactionIdBeforeLoad
            )
        }
        launch {
            googleSheetsExporter.export(
                GoogleSheetsSink.fromProperties(properties),
                transactionIdBeforeLoad
            )
        }
    }
}
