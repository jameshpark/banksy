package org.jameshpark.banksy.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
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

    override suspend fun  executeBatch (sql: String, batchParams: List<List<Any?>>): List<Int> {
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

    override suspend fun <T> query(sql: String, params: List<Any?>, transform: ResultSet.() -> T): Flow<T> {
        return prepareStatement(sql, params).use {
            flow {
                val rs = it.executeQuery()
                while (rs.next()) {
                    emit(rs.transform())
                }
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


