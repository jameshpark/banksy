package org.jameshpark.banksy.extractor

import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class CsvExtractor(private val reader: CsvReader = csvReader()) : Extractor {
    override suspend fun extractTransactionData(sourcePath: String): Flow<Map<String, String>> {
        return extractRowsFromCsvs(sourcePath)
    }

    private fun extractRowsFromCsvs(sourceDirectory: String): Flow<Map<String, String>> = flow {
        val directory = File(sourceDirectory)

        if (directory.exists() && directory.isDirectory) {
            directory.walk().filter { it.isFile && it.extension.lowercase() == "csv" }.forEach { file ->
                logger.info { "Extracting rows from file '$file'" }
                val counter = AtomicInteger(0)
                reader.readAllWithHeader(file).forEach { row ->
                    if (counter.getAndIncrement() % 100 == 0) {
                        logger.info { "Extracted ${counter.get()} rows from file '$file'" }
                    }
                    emit(row)
                }
                logger.info { "Extracted a total of ${counter.get()} rows from file '$file'" }
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}