package org.jameshpark.banksy.extractor

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jameshpark.banksy.clients.TellerClient
import org.jameshpark.banksy.database.Dao
import org.jameshpark.banksy.models.Extracted
import org.jameshpark.banksy.models.TellerExtracted
import org.jameshpark.banksy.models.TellerFeed
import java.time.LocalDate

class TellerExtractor(
    private val dao: Dao,
    private val tellerClient: TellerClient
) : Extractor<TellerFeed> {
    override suspend fun extract(feed: TellerFeed): Flow<Extracted> {
        val bookmarkName = feed.getBookmarkName()
        val previousBookmark = dao.getLatestBookmarkByName(bookmarkName) ?: LocalDate.EPOCH
        val tellerTransactions = tellerClient.getTransactions(feed.accountId, feed.accessToken, previousBookmark)
        return tellerTransactions.map { TellerExtracted(it) }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}