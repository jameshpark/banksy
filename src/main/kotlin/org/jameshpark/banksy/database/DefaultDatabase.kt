package org.jameshpark.banksy.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.sql.*
import java.time.LocalDate
import javax.lang.model.type.NullType

class DefaultDatabase(private val conn: Connection) : Database {
    override suspend fun execute(sql: String, params: List<Any?>): Int {
        return prepareStatement(sql, params).use {
            withContext(Dispatchers.IO) {
                async {
                    it.executeUpdate()
                }.await()
            }
        }
    }

    override suspend fun executeBatch(sql: String, batchParams: List<List<Any?>>): List<Int> {
        val ps = prepareBatchStatement(sql, batchParams)
        val updateCounts = ps.use {
            withContext(Dispatchers.IO) {
                async {
                    it.executeBatch()
                }.await()
            }
        }
        return updateCounts.toList()
    }

    override suspend fun <T> query(sql: String, params: List<Any?>, transform: ResultSet.() -> T): Flow<T> = flow {
        prepareStatement(sql, params).use { preparedStatement ->
            val rs = withContext(Dispatchers.IO) {
                async {
                    preparedStatement.executeQuery()
                }.await()
            }

            while (rs.next()) {
                val result = rs.transform()
                emit(result)
            }
        }
    }

    private fun prepareStatement(sql: String, params: List<Any?>) = conn.prepareStatement(sql).apply {
        mapParameters(params)
    }

    private fun PreparedStatement.mapParameters(params: List<Any?>) {
        params.forEachIndexed { i, param ->
            val sqlIndex = i + 1
            when (param) {
                is String -> setString(sqlIndex, param)
                is Int -> setInt(sqlIndex, param)
                is Long -> setLong(sqlIndex, param)
                is Double -> setDouble(sqlIndex, param)
                is BigDecimal -> setBigDecimal(sqlIndex, param)
                is Boolean -> setBoolean(sqlIndex, param)
                is LocalDate -> setDate(sqlIndex, Date.valueOf(param))
                is NullType -> setNull(sqlIndex, Types.NULL)
                else -> throw IllegalArgumentException("Unsupported parameter type for param '$param'")
            }
        }
    }

    private fun prepareBatchStatement(sql: String, params: List<List<Any?>>) = conn.prepareStatement(sql).apply {
        params.forEach {
            mapParameters(it)
            addBatch()
        }
    }
}