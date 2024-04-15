package org.jameshpark.banksy.extractor

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jameshpark.banksy.transformer.headersToMapper
import org.jameshpark.banksy.models.Transaction
import org.jameshpark.banksy.models.toTransaction
import java.io.File

object ExtractorEager {

    suspend fun extractFromCsv(directoryName: String): List<Transaction> {
        val rows = extractRows(directoryName)
        return parseTransactions(rows)
    }

    private fun extractRows(directoryName: String): List<Map<String, String>> {
        val reader = csvReader()
        val directory = File(directoryName)

        return if (directory.exists() && directory.isDirectory) {
            directory.walk().toList().filter { it.isFile && it.extension.lowercase() == "csv" }.flatMap { file ->
                reader.readAllWithHeader(file)
            }
        } else {
            throw IllegalArgumentException("Directory '$directoryName' does not exist or is not a directory")
        }
    }

    private suspend fun parseTransactions(rows: List<Map<String, String>>): List<Transaction> = rows.map { row ->
        val mapper = headersToMapper[row.keys]
        if (mapper != null) {
            row.toTransaction(mapper)
        } else {
            logger.info { "No mapper for headers '${row.keys}', skipping row '${row.values}'" }
            null
        }
    }.filterNotNull().sortedBy { it.description }

    private val logger = KotlinLogging.logger { }
}
