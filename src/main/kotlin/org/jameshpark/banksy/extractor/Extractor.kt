package org.jameshpark.banksy.extractor

import kotlinx.coroutines.flow.Flow

interface Extractor {
    suspend fun extractTransactionData(sourcePath: String): Flow<Map<String, String>>
}