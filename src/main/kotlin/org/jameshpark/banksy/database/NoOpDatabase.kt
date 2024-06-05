package org.jameshpark.banksy.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.sql.ResultSet

class NoOpDatabase : Database {
    override suspend fun execute(sql: String, params: List<Any?>): Int = 0

    override suspend fun executeBatch(sql: String, batchParams: List<List<Any?>>): List<Int> = emptyList()

    override suspend fun <T> query(sql: String, params: List<Any?>, transform: ResultSet.() -> T): Flow<T> = emptyFlow()
}