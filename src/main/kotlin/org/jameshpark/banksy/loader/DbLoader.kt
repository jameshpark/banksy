package org.jameshpark.banksy.loader

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jameshpark.banksy.database.Database
import org.jameshpark.banksy.models.Transaction

class DbLoader(private val db: Database) : Loader {
    override suspend fun saveTransactions(transactions: Flow<Transaction>) {
        val sql = """
            INSERT OR IGNORE INTO transactions (date, description, amount, category, type, originHash)
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        transactions.chunked(500).collect { chunk ->
            val batchParams = chunk.map { it.toDbRow() }
            db.executeBatch(sql, batchParams)
        }
    }

    private suspend fun createTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date DATE NOT NULL,
                description TEXT NOT NULL,
                amount NUMERIC NOT NULL,
                category TEXT NOT NULL,
                type TEXT NOT NULL,
                originHash TEXT NOT NULL
            );
        """.trimIndent()
        db.execute(sql)
    }
}

fun <T> Flow<T>.chunked(size: Int): Flow<List<T>> = flow {
    val chunk = mutableListOf<T>()
    collect {
        chunk.add(it)
        if (chunk.size >= size) {
            emit(chunk.toList())
            chunk.clear()
        }
    }
    if (chunk.isNotEmpty()) {
        emit(chunk)
    }
}