package org.jameshpark.banksy.database

import kotlinx.coroutines.flow.Flow
import java.sql.ResultSet

interface Database {

    suspend fun execute(sql: String, params: List<Any?> = emptyList()): Int

    suspend fun executeBatch(sql: String, batchParams: List<List<Any?>> = emptyList()): List<Int>

    suspend fun <T> query(sql: String, params: List<Any?> = emptyList(), transform: ResultSet.() -> T): Flow<T>

}