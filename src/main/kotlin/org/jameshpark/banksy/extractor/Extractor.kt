package org.jameshpark.banksy.extractor

import kotlinx.coroutines.flow.Flow
import org.jameshpark.banksy.models.Extracted
import org.jameshpark.banksy.models.Feed

interface Extractor<T : Feed> {

    suspend fun extract(feed: T): Flow<Extracted>

}
