package org.jameshpark.banksy.extractor

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jameshpark.banksy.clients.TellerClient
import org.jameshpark.banksy.database.Dao
import org.jameshpark.banksy.models.Extracted
import org.jameshpark.banksy.models.TellerExtracted
import org.jameshpark.banksy.models.TellerFeed

class TellerExtractor(
    private val dao: Dao,
    private val tellerClient: TellerClient
) : Extractor<TellerFeed> {
    override suspend fun extract(feed: TellerFeed, fromDate: LocalDate?): Flow<Extracted> {
        val bookmark = fromDate ?: run {
            val bookmarkName = feed.getBookmarkName()
            dao.getLatestBookmarkByName(bookmarkName) ?: LocalDate.EPOCH
        }

        val tellerTransactions = tellerClient.getTransactions(feed.accountId, feed.accessToken, bookmark)
        return tellerTransactions.map { TellerExtracted(it, feed.feedName) }
    }
}