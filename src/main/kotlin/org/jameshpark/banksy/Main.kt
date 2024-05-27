package org.jameshpark.banksy

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jameshpark.banksy.database.Dao
import org.jameshpark.banksy.exporter.CsvExporter
import org.jameshpark.banksy.exporter.GoogleSheetsExporter
import org.jameshpark.banksy.extractor.CsvExtractor
import org.jameshpark.banksy.loader.DefaultLoader
import org.jameshpark.banksy.models.CsvFeed
import org.jameshpark.banksy.models.CsvSink
import org.jameshpark.banksy.models.GoogleSheetsSink
import org.jameshpark.banksy.transformer.DefaultTransformer
import org.jameshpark.banksy.utils.sheetsServiceFromCredentials
import java.io.File
import java.time.Instant

const val SOURCE_DIRECTORY = "transactions"

fun main() = runBlocking {
    val dao = Dao.fromUrl("jdbc:sqlite:database.db")
    val extractor = CsvExtractor(dao)
    val transformer = DefaultTransformer()
    val loader = DefaultLoader(dao)
    val csvExporter = CsvExporter(dao)
    val googleSheetsExporter = GoogleSheetsExporter(dao, sheetsServiceFromCredentials("REPLACE_ME"))

    val directory = File(SOURCE_DIRECTORY)
    val csvFeeds = if (directory.exists() && directory.isDirectory) {
        directory.walk().filter { it.isFile && it.extension.lowercase() == "csv" }.map { CsvFeed(it) }.toList()
    } else {
        emptyList()
    }

    val transactionIdBeforeLoad = dao.getLatestTransactionId()

    coroutineScope {
        csvFeeds.map { feed ->
            launch {
                val rows = extractor.extract(feed)
                val transactions = transformer.transform(rows, feed.file.name)
                loader.saveTransactions(feed, transactions)
            }
        }
    }

    val filePath = "exports/export_${Instant.now().epochSecond}.csv"
    csvExporter.export(CsvSink(filePath), transactionIdBeforeLoad)
    googleSheetsExporter.export(GoogleSheetsSink("spreadsheetId", "Transactions!A2"), transactionIdBeforeLoad)
}
