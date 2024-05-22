package org.jameshpark.banksy.loader

import kotlinx.coroutines.flow.Flow
import org.jameshpark.banksy.models.Feed
import org.jameshpark.banksy.models.Transaction

interface Loader {

    suspend fun saveTransactions(feed: Feed, transactions: Flow<Transaction>)

    suspend fun exportToCsv(feed: Feed, filePath: String, includeHeader: Boolean = false)
}