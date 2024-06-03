package org.jameshpark.banksy.models

import java.io.File

data class CsvFeed(val file: File) : Feed {

    override fun getBookmarkName(): String = file.nameWithoutExtension.lowercase().run {
        val feedName = when {
            contains("gold") -> FeedName.AMEX_GOLD
            contains("plat") -> FeedName.AMEX_PLATINUM
            contains("chase0000") -> FeedName.CHASE_SAPPHIRE
            contains("chase2002") -> FeedName.CHASE_CHECKING
            contains("chase3149") -> FeedName.CHASE_FREEDOM_UNLIMITED
            contains("chase7959") -> FeedName.CHASE_FREEDOM
            else -> throw IllegalArgumentException("No bookmark key configured for '${file.name}'")
        }
        feedName.name
    }
}