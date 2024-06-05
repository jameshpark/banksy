package org.jameshpark.banksy.database

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.sql.ResultSet

class NoOpDatabase(private val verbose: Boolean = false) : Database {
    override suspend fun execute(sql: String, params: List<Any?>): Int {
        if (verbose) {
            logger.info { "No-op execute sql: $sql, params: $params" }
        }
        return 0
    }

    override suspend fun executeBatch(sql: String, batchParams: List<List<Any?>>): List<Int> {
        if (verbose) {
            logger.info { "No-op executeBatch: $sql, batchParams: $batchParams" }
        }
        return emptyList()
    }

    override suspend fun <T> query(sql: String, params: List<Any?>, transform: ResultSet.() -> T): Flow<T> {
        if (verbose) {
            logger.info { "No-op query sql: $sql, params: $params" }
        }
        return emptyFlow()
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}