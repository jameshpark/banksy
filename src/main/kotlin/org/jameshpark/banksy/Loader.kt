package org.jameshpark.banksy

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.jameshpark.banksy.models.Transaction
import java.io.File

object Loader {
    suspend fun loadTransactions(transactions: Flow<Transaction>) {
        val database = File("database.csv")

        // check if database.csv exists, if not create it
        if (!database.exists()) {
            withContext(Dispatchers.IO) {
                database.createNewFile()
                database.writeText("date,description,amount,category,type,originHash\n")
            }

        }
        csvWriter().openAsync(database, append = true) {
            transactions.collect {
                writeRow(it.toRow())
            }
        }
    }
}