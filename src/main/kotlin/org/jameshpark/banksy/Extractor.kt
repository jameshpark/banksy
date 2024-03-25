package org.jameshpark.banksy

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun parseTransactions(rows: Flow<Map<String, String>>): Flow<Transaction> = rows.flatMapMerge { row ->
        flow {
            val mapper = headersToMapper[row.keys]

            if (mapper != null) {
                emit(row.toTransaction(mapper))
            } else {
                println("No mapper for headers '${row.keys}', skipping row '${row.values}'")
                emit(null)
            }
        }
    }.filterNotNull()
}
