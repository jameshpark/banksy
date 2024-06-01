package org.jameshpark.banksy

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
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
import org.jameshpark.banksy.utils.ApplicationScope
import org.jameshpark.banksy.utils.csvFeedsFromProperties
import org.jameshpark.banksy.utils.launchApp
import org.jameshpark.banksy.utils.sheetsServiceFromProperties

class Banksy : CliktCommand() {

    private val exportToGoogleSheet by option().boolean().default(false)
        .help("Set true to export results to Google Sheets. Default: false")

    override fun run() = launchApp {
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

        coroutineScope {
            exportToCsv(this, dao, this@launchApp, transactionIdBeforeLoad)

            if (exportToGoogleSheet) {
                exportToGoogleSheet(this, dao, this@launchApp, transactionIdBeforeLoad)
            }
        }
    }

    /**
     * Processes a list of CSV feeds concurrently.
     *
     * Data from each CSV feed is extracted, filtered, transformed, and then loaded into a local database.
     *
     * @param csvFeeds The list of CSV feeds to process.
     * @param dao The DataAccessObject used for saving transactions and retrieving bookmarks.
     */
    suspend fun processFeeds(
        csvFeeds: List<CsvFeed>,
        dao: Dao
    ) {
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

    private fun exportToCsv(
        coroutineScope: CoroutineScope,
        dao: Dao,
        applicationScope: ApplicationScope,
        transactionIdBeforeLoad: Int
    ) {
        coroutineScope.launch {
            CsvExporter(dao).export(
                CsvSink.fromProperties(applicationScope.properties),
                transactionIdBeforeLoad
            )
        }
    }

    private fun exportToGoogleSheet(
        coroutineScope: CoroutineScope,
        dao: Dao,
        applicationScope: ApplicationScope,
        transactionIdBeforeLoad: Int
    ) {
        coroutineScope.launch {
            GoogleSheetsExporter(
                dao,
                sheetsServiceFromProperties(applicationScope.properties)
            ).export(
                GoogleSheetsSink.fromProperties(applicationScope.properties),
                transactionIdBeforeLoad
            )
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}