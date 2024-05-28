package org.jameshpark.banksy.database

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.jameshpark.banksy.models.Category
import org.jameshpark.banksy.models.Transaction
import org.jameshpark.banksy.models.TransactionType
import org.jameshpark.banksy.utils.require
import java.sql.DriverManager
import java.time.Instant
import java.time.LocalDate
import java.util.*

class Dao(private val db: Database) {

    suspend fun getLatestBookmarkByName(name: String): LocalDate? {
        val sql = "SELECT bookmark FROM bookmarks WHERE name = ? ORDER BY bookmark DESC LIMIT 1"
        return db.query(
            sql,
            listOf(name)
        ) {
            this.getDate("bookmark")
        }.firstOrNull()?.toLocalDate()
    }

    suspend fun saveBookmark(name: String, bookmark: LocalDate) {
        val sql = "INSERT INTO bookmarks (name, bookmark, run_timestamp) VALUES (?, ?, ?)"
        val params = listOf(name, bookmark, Instant.now().toEpochMilli())
        db.execute(sql, params)
    }

    suspend fun saveTransactions(transactions: List<Transaction>) {
        val sql = """
            INSERT OR IGNORE INTO transactions (date, description, amount, category, critical, type, originHash)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        val batchParams = transactions.map { it.toDbRow() }
        db.executeBatch(sql, batchParams)
    }

    suspend fun getLatestTransactionId(): Int {
        val sql = """
            SELECT MAX(id) AS id
            FROM transactions
        """.trimIndent()

        return db.query(sql) {
            getInt("id")
        }.firstOrNull() ?: 0
    }

    suspend fun getTransactionsNewerThanId(id: Int): Flow<Transaction> {
        val sql = """
            SELECT date
                 , description
                 , amount
                 , category
                 , critical
                 , type
                 , originHash
            FROM transactions
            WHERE id > ?
            ORDER BY date DESC
        """.trimIndent()

        val params = listOf(id)

        return db.query(sql, params) {
            Transaction(
                date = getDate("date").toLocalDate(),
                description = getString("description"),
                amount = getBigDecimal("amount"),
                category = Category.valueOf(getString("category")),
                type = TransactionType.valueOf(getString("type")),
                originHash = getString("originHash")
            )
        }

    }

    suspend fun initializeDatabase() {
        createTransactionsTable()
        createBookmarksTable()
        logger.info { "Initialized database" }
    }

    private suspend fun createTransactionsTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date INTEGER NOT NULL,
                description TEXT NOT NULL,
                amount REAL NOT NULL,
                category TEXT NOT NULL,
                critical INTEGER NOT NULL,
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
                name STRING NOT NULL,
                bookmark INTEGER NOT NULL,
                run_timestamp INTEGER NOT NULL
            );
        """.trimIndent()
        db.execute(sql)
    }

    companion object {

        private val logger = KotlinLogging.logger { }

        fun fromProperties(properties: Properties) = fromUrl(properties.require("app.database.url"))

        private fun fromUrl(url: String): Dao {
            val dbConnection = DriverManager.getConnection(url)
            val db = DefaultDatabase(dbConnection)
            logger.info { "Connected to database" }
            return Dao(db)
        }

    }
}
