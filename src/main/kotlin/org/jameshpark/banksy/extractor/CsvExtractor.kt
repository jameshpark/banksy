package org.jameshpark.banksy.extractor

import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import org.jameshpark.banksy.database.Dao
import org.jameshpark.banksy.models.CsvFeed
import org.jameshpark.banksy.transformer.headersToMapper
import java.io.File
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

class CsvExtractor(
    private val dao: Dao,
    private val reader: CsvReader = csvReader()
) : Extractor<CsvFeed> {

    override suspend fun extract(feed: CsvFeed): Flow<Map<String, String>> {
        val (rows, newBookmark) = readCsvRows(feed.file)
        val bookmarkName = feed.getBookmarkName()
        val previousBookmark = dao.getLatestBookmarkByName(bookmarkName) ?: LocalDate.EPOCH
        logger.info { "Filtering $bookmarkName transactions older than $previousBookmark." }
        val rowsSincePreviousBookmark = filterRowsSinceBookmark(previousBookmark, rows)
        logger.info { "Saving new $bookmarkName bookmark $newBookmark" }
        dao.saveBookmark(bookmarkName, newBookmark)
        return rowsSincePreviousBookmark
    }

    private fun readCsvRows(file: File): Pair<Flow<Map<String, String>>, LocalDate> {
        require(file.isFile && file.extension.lowercase() == "csv") { "${file.name} is not a csv file" }

        val counter = AtomicInteger(0)
        var bookmark = LocalDate.EPOCH

        logger.info { "Extracting rows from file '$file'" }
        val rows = flow {
            reader.readAllWithHeader(file).forEach { row ->
                if (counter.getAndIncrement() % 100 == 0) {
                    logger.info { "Extracted ${counter.get()} rows from file '$file'" }
                }

                getTransactionDate(row).also {
                    if (it.isAfter(bookmark)) {
                        bookmark = it
                    }
                }

                emit(row)
            }
        }
        logger.info { "Extracted a total of ${counter.get()} rows from file '$file' with new bookmark '$bookmark'" }

        return rows to bookmark
    }

    private fun getTransactionDate(row: Map<String, String>): LocalDate {
        val mapper = mapperFromKeys(row.keys)
        return LocalDate.parse(row[mapper.dateColumn]!!, mapper.dateTimeFormatter)
    }

    private fun mapperFromKeys(keys: Set<String>) =
        headersToMapper[keys] ?: throw IllegalArgumentException("No mapper configured for headers '$keys'")

    private fun filterRowsSinceBookmark(
        bookmark: LocalDate,
        rows: Flow<Map<String, String>>
    ): Flow<Map<String, String>> {
        return rows.filter { row ->
            val keys = row.keys
            val mapper =
                headersToMapper[keys] ?: throw IllegalArgumentException("No mapper configured for headers '$keys'")
            val transactionDate = LocalDate.parse(row[mapper.dateColumn]!!, mapper.dateTimeFormatter)
            transactionDate > bookmark
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }

}