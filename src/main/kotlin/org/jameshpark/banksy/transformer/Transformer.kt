package org.jameshpark.banksy.transformer

import kotlinx.coroutines.flow.*
import org.jameshpark.banksy.models.Transaction

interface Transformer {
    suspend fun transform(rows: Flow<Map<String, String>>, sourceName: String? = null): Flow<Transaction>
}
