package org.jameshpark.banksy.models

data class TellerFeed(
    val feedName: FeedName,
    val accessToken: String,
    val accountId: String,
) : Feed {
    override fun getBookmarkName(): String {
        return feedName.name
    }
}
