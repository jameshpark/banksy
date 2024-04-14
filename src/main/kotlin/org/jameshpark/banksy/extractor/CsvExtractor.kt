package org.jameshpark.banksy.extractor

import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class CsvExtractor(private val reader: CsvReader = csvReader()) : Extractor {
    override suspend fun extractTransactionData(sourcePath: String): Flow<Map<String, String>> {
        return extractRowsFromCsvs(sourcePath)
    }

    private fun extractRowsFromCsvs(sourceDirectory: String): Flow<Map<String, String>> = flow {
        val directory = File(sourceDirectory)

        if (directory.exists() && directory.isDirectory) {
            directory.walk().filter { it.isFile && it.extension.lowercase() == "csv" }.forEach { file ->
                reader.readAllWithHeader(file).forEach { row ->
                    emit(row) // Emit each row as a Map<String, String>
                }
            }
        }
    }
}