package org.jameshpark.banksy.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.jameshpark.banksy.models.Category
import org.jameshpark.banksy.models.Transaction
import org.jameshpark.banksy.models.TransactionType
import org.jameshpark.banksy.utils.require
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

    companion object {
        fun fromProperties(properties: Properties): Dao {
            val db = DefaultDatabase.fromUrl(properties.require("app.database.url"))
            return Dao(db)
        }
    }
}
