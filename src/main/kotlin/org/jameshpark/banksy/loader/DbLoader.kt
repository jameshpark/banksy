package org.jameshpark.banksy.loader

import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jameshpark.banksy.database.Database
import org.jameshpark.banksy.database.DefaultDatabase
import org.jameshpark.banksy.models.Category
import org.jameshpark.banksy.models.Transaction
import org.jameshpark.banksy.models.TransactionType
import java.io.File
import java.sql.DriverManager
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

class DbLoader(private val db: Database, private val writer: CsvWriter = csvWriter()) : Loader {
    override suspend fun saveTransactions(transactions: Flow<Transaction>) {
        println("Entered saveTransactions")
        val sql = """
            INSERT OR IGNORE INTO transactions (date, description, amount, category, type, originHash)
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        println("val sql")
        // filter transactions to those newer than the current bookmark
        val bookmark = getCurrentBookmark() ?: LocalDate.EPOCH
        logger.info { "Current bookmark: $bookmark" }

        // keep track of the latest transaction date seen
        var latestTransactionDate = LocalDate.EPOCH

        println("before filtering transactions")
        val counter = AtomicInteger(0)
        transactions.filter {
            it.date.isAfter(bookmark)
        }.chunked(500).collect { chunk ->
            println("processing chunk")
            // save the latest transaction date seen
            chunk.maxByOrNull { it.date }?.let {
                if (it.date.isAfter(latestTransactionDate)) {
                    latestTransactionDate = it.date
                }
            }

            println("before saving chunk to db")
            // save to db
            val batchParams = chunk.map { it.toDbRow() }
            db.executeBatch(sql, batchParams)

            logger.info { "Saved ${counter.addAndGet(chunk.size)} transactions" }
        }

        // only write this if we actually had transactions
        if (latestTransactionDate > LocalDate.EPOCH) {
            saveBookmark(latestTransactionDate)
            logger.info { "Saved bookmark: $latestTransactionDate" }
        }
    }

    override suspend fun exportToCsv(filePath: String) {
        logger.info { "Exporting to $filePath" }
        val output = File(filePath)

        // check if database.csv exists, if not create it
        withContext(Dispatchers.IO) {
            if (output.exists()) {
                launch {
                    output.delete()
                }.join()
            } else {
                launch {
                    output.createNewFile()
                    output.writeText("date,description,amount,category,type,originHash\n")
                }.join()
            }
        }

        val sql = """
            SELECT date
                 , description
                 , amount
                 , category
                 , type
                 , originHash
            FROM transactions
            ORDER BY date DESC
        """.trimIndent()

        val transactions = db.query(sql) {
            Transaction(
                date = getDate("date").toLocalDate(),
                description = getString("description"),
                amount = getBigDecimal("amount"),
                category = Category.valueOf(getString("category")),
                type = TransactionType.valueOf(getString("type")),
                originHash = getString("originHash")
            )
        }

        writer.openAsync(output, append = true) {
            transactions.collect {
                writeRow(it.toCsvRow())
            }
        }
        logger.info { "Exported transactions to $filePath" }
    }

    suspend fun initializeDatabase() {
        createTransactionsTable()
        createBookmarksTable()
        logger.info { "Initialized database" }
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
            logger.info { "Connected to database" }
            return DbLoader(db)
        }
    }
}

fun <T> Flow<T>.chunked(size: Int): Flow<List<T>> = flow {
    val chunk = mutableListOf<T>()
    collect {
        println("collecting into chunk")
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