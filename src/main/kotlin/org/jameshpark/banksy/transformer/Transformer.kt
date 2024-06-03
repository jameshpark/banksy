package org.jameshpark.banksy.transformer

import kotlinx.coroutines.flow.*
import org.jameshpark.banksy.models.Extracted
import org.jameshpark.banksy.models.Transaction

interface Transformer {

    suspend fun transform(rows: Flow<Extracted>, sourceName: String? = null): Flow<Transaction>

}
