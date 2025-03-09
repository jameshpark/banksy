package org.jameshpark.banksy.extractor

import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.jameshpark.banksy.database.Dao
import org.jameshpark.banksy.models.CsvExtracted
import org.jameshpark.banksy.models.CsvFeed
import org.jameshpark.banksy.models.Extracted
import org.jameshpark.banksy.transformer.headersToMapper

class CsvExtractor(
    private val dao: Dao,
    private val reader: CsvReader = csvReader()
) : Extractor<CsvFeed> {

    override suspend fun extract(feed: CsvFeed, fromDate: LocalDate?): Flow<Extracted> {
        val rows = readCsvRows(feed.file)
        val bookmarkName = feed.getBookmarkName()
        val previousBookmark = dao.getLatestBookmarkByName(bookmarkName) ?: LocalDate.EPOCH
        val rowsSincePreviousBookmark = filterRowsSinceBookmark(previousBookmark, rows)
        return rowsSincePreviousBookmark.map { CsvExtracted(it) }
    }

    private suspend fun readCsvRows(file: File): Flow<Map<String, String>> {
        require(file.isFile && file.extension.lowercase() == "csv") { "${file.name} is not a csv file" }

        return withContext(Dispatchers.IO) {
            reader.readAllWithHeader(file)
        }.asFlow()
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

            (transactionDate > bookmark).also { transactionDateIsAfterBookmark ->
                if (!transactionDateIsAfterBookmark) {
                    logger.info { "Filtered out '$row' because transaction date <= $bookmark" }
                }
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}