package org.jameshpark.banksy

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.flow.*
import java.io.File

object Extractor {

    fun extractTransactionData(directoryName: String): Flow<Map<String, String>> = extractRowsFromCsvs(directoryName)

    private fun extractRowsFromCsvs(directoryName: String): Flow<Map<String, String>> = flow {
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

}
