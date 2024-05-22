package org.jameshpark.banksy

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jameshpark.banksy.database.Dao
import org.jameshpark.banksy.extractor.CsvExtractor
import org.jameshpark.banksy.loader.DefaultLoader
import org.jameshpark.banksy.models.CsvFeed
import org.jameshpark.banksy.transformer.DefaultTransformer
import java.io.File
import java.time.Instant

const val SOURCE_DIRECTORY = "transactions"

fun main(): Unit = runBlocking {
    val dao = Dao.fromUrl("jdbc:sqlite:database.db")
    val extractor = CsvExtractor(dao)
    val transformer = DefaultTransformer()
    val loader = DefaultLoader(dao)

    val directory = File(SOURCE_DIRECTORY)
    val csvFeeds = if (directory.exists() && directory.isDirectory) {
        directory.walk().filter { it.isFile && it.extension.lowercase() == "csv" }.map { CsvFeed(it) }.toList()
    } else {
        emptyList()
    }

    coroutineScope {
        csvFeeds.map { feed ->
            launch {
                val rows = extractor.extract(feed)
                val transactions = transformer.transform(rows, feed.file.name)
                loader.saveTransactions(feed, transactions)
                val fileName = "export_${feed.getBookmarkName()}_${Instant.now().epochSecond}.csv"
                loader.exportToCsv(feed, "exports/$fileName")
            }
        }
    }
}
