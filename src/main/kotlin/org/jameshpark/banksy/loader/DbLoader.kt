package org.jameshpark.banksy.loader

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.*
import org.jameshpark.banksy.database.Database
import org.jameshpark.banksy.database.DefaultDatabase
import org.jameshpark.banksy.models.Transaction
import java.sql.DriverManager
import java.time.Instant
import java.time.LocalDate

class DbLoader(private val db: Database) : Loader {
    override suspend fun saveTransactions(transactions: Flow<Transaction>) {
        val sql = """
            INSERT OR IGNORE INTO transactions (date, description, amount, category, type, originHash)
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        // filter transactions to those newer than the current bookmark
        val bookmark = getCurrentBookmark() ?: LocalDate.EPOCH

        // keep track of the latest transaction date seen
        var latestTransactionDate = LocalDate.EPOCH

        transactions.filter {
            it.date.isAfter(bookmark)
        }.chunked(500).collect { chunk ->
            // save the latest transaction date seen
            chunk.maxByOrNull { it.date }?.let {
                if (it.date.isAfter(latestTransactionDate)) {
                    latestTransactionDate = it.date
                }
            }

            // save to db
            val batchParams = chunk.map { it.toDbRow() }
            db.executeBatch(sql, batchParams)
        }

        // only write this if we actually had transactions
        if (latestTransactionDate > LocalDate.EPOCH) {
            saveBookmark(latestTransactionDate)
        }
    }

    suspend fun initializeDatabase() {
        createTransactionsTable()
        createBookmarksTable()
    }

    private suspend fun getCurrentBookmark(): LocalDate? {
        val sql = "SELECT bookmark FROM bookmarks ORDER BY bookmark DESC LIMIT 1"
        return db.query(sql) {
            this.getDate("bookmark")
        }.firstOrNull()?.toLocalDate()
    }

    private suspend fun saveBookmark(bookmark: LocalDate) {
        val sql = "INSERT INTO bookmarks (run_timestamp, bookmark) VALUES (?, ?)"
        val params = listOf(Instant.now().toEpochMilli(), bookmark)
        db.execute(sql, params)
    }

    private suspend fun createTransactionsTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date INTEGER NOT NULL,
                description TEXT NOT NULL,
                amount REAL NOT NULL,
                category TEXT NOT NULL,
                type TEXT NOT NULL,
                originHash TEXT NOT NULL
            );
        """.trimIndent()
        db.execute(sql)
    }

    private suspend fun createBookmarksTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS bookmarks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                run_timestamp INTEGER NOT NULL,
                bookmark INTEGER NOT NULL
            );
        """.trimIndent()
        db.execute(sql)
    }

    companion object {
        private val logger = KotlinLogging.logger { }

        fun fromUrl(url: String): DbLoader {
            val dbConnection = DriverManager.getConnection(url)
            val db = DefaultDatabase(dbConnection)
            return DbLoader(db)
        }
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