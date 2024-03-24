package org.jameshpark.banksy

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.jameshpark.org.jameshpark.banksy.models.Transaction
import org.jameshpark.org.jameshpark.banksy.models.toTransaction
import java.io.File

object Extractor {

    fun extractFromCsv(directoryName: String): Flow<Transaction> {
        val rows = extractRows(directoryName)
        return parseTransactions(rows)
    }

    private fun extractRows(directoryName: String): Flow<Map<String, String>> = flow {
        val reader = csvReader()
        val directory = File(directoryName)

        if (directory.exists() && directory.isDirectory) {
            directory.walk().filter { it.isFile && it.extension.lowercase() == "csv" }.forEach { file ->
                reader.readAllWithHeader(file).forEach { row ->
                    emit(row) // Emit each row as a Map<String, String>
                }
            }
        }
    }

    private fun parseTransactions(rows: Flow<Map<String, String>>): Flow<Transaction> = rows.map { row ->
        val mapper = headersToMapper[row.keys]
        if (mapper != null) {
            row.toTransaction(mapper)
        } else {
            println("No mapper for headers '${row.keys}', skipping row '${row.values}'")
            null
        }
    }.filterNotNull()
}
