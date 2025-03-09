package org.jameshpark.banksy.extractor

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import org.jameshpark.banksy.models.Extracted
import org.jameshpark.banksy.models.Feed

sealed interface Extractor<T : Feed> {

    suspend fun extract(feed: T, fromDate: LocalDate? = null): Flow<Extracted>

}
