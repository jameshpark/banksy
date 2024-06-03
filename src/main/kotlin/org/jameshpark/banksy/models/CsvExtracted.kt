package org.jameshpark.banksy.models

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jameshpark.banksy.transformer.headersToMapper

data class CsvExtracted(val row: Map<String, String>) : Extracted {
    override suspend fun toTransaction(): Transaction? {
        val mapper = headersToMapper[row.keys]
        return if (mapper != null) {
            row.toTransaction(mapper)
        } else {
            logger.warn { "No mapper for headers '${row.keys}', skipping row '${row.values}'" }
            null
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}