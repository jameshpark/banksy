package org.jameshpark.banksy.models

import java.io.File

interface Feed {

    fun getBookmarkName(): String

}

class CsvFeed(val file: File) : Feed {

    override fun getBookmarkName(): String = file.nameWithoutExtension.lowercase().run {
        when {
            contains("gold") -> AMEX_GOLD
            contains("plat") -> AMEX_PLATINUM
            contains("chase0000") -> CHASE_SAPPHIRE
            contains("chase2002") -> CHASE_CHECKING
            contains("chase3149") -> CHASE_FREEDOM_UNLIMITED
            contains("chase7959") -> CHASE_FREEDOM
            else -> throw IllegalArgumentException("No bookmark key configured for '${file.name}'")
        }
    }

    companion object {
        private const val AMEX_GOLD = "AMEX_GOLD"
        private const val AMEX_PLATINUM = "AMEX_PLATINUM"
        private const val CHASE_SAPPHIRE = "CHASE_SAPPHIRE"
        private const val CHASE_CHECKING = "CHASE_CHECKING"
        private const val CHASE_FREEDOM_UNLIMITED = "CHASE_FREEDOM_UNLIMITED"
        private const val CHASE_FREEDOM = "CHASE_FREEDOM"
    }

}
