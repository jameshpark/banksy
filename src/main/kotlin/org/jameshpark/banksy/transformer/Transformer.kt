package org.jameshpark.banksy.transformer

import kotlinx.coroutines.flow.Flow
import org.jameshpark.banksy.models.Transaction

interface Transformer {

    suspend fun transform(rows: Flow<Map<String, String>>): Flow<Transaction>
}
