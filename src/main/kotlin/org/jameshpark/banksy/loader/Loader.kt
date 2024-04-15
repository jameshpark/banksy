package org.jameshpark.banksy.loader

import kotlinx.coroutines.flow.Flow
import org.jameshpark.banksy.models.Transaction

interface Loader {

    suspend fun saveTransactions(transactions: Flow<Transaction>)

    suspend fun exportToCsv(filePath: String)
}