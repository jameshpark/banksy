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
import org.jameshpark.banksy.models.CsvSink
import org.jameshpark.banksy.models.GoogleSheetsSink
import org.jameshpark.banksy.transformer.DefaultTransformer
import org.jameshpark.banksy.utils.csvFeedsFromProperties
import org.jameshpark.banksy.utils.launchApp
import org.jameshpark.banksy.utils.sheetsServiceFromProperties

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
