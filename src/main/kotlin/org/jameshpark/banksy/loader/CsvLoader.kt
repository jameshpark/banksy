package org.jameshpark.banksy.loader

import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.jameshpark.banksy.models.Transaction
import java.io.File

class CsvLoader(private val writer: CsvWriter = csvWriter()) : Loader {
    override suspend fun saveTransactions(transactions: Flow<Transaction>) {
        val database = File("database.csv")

        // check if database.csv exists, if not create it
        if (!database.exists()) {
            withContext(Dispatchers.IO) {
                database.createNewFile()
                database.writeText("date,description,amount,category,type,originHash\n")
            }

        }
        writer.openAsync(database, append = true) {
            transactions.collect {
                writeRow(it.toCsvRow())
            }
        }
    }

    override suspend fun exportToCsv(filePath: String) {
        TODO("Not yet implemented")
    }
}