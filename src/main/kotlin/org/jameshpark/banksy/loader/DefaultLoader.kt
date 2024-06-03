package org.jameshpark.banksy.loader

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import org.jameshpark.banksy.database.Dao
import org.jameshpark.banksy.models.Feed
import org.jameshpark.banksy.models.Transaction
import org.jameshpark.banksy.utils.chunked
import java.time.LocalDate

class DefaultLoader(private val dao: Dao) : Loader {

    override suspend fun saveTransactions(feed: Feed, transactions: Flow<Transaction>) {
        var newBookmark: LocalDate = LocalDate.EPOCH
        transactions.chunked(500).collect { chunk ->
            dao.saveTransactions(chunk)
            logger.info { "Saved ${chunk.size} transactions" }
            chunk.maxByOrNull { it.date }!!.date.also {
                if (it > newBookmark) {
                    newBookmark = it
                }
            }
        }

        if (newBookmark > LocalDate.EPOCH) {
            dao.saveBookmark(feed.getBookmarkName(), newBookmark)
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}
