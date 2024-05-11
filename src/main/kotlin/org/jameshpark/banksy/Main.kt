package org.jameshpark.banksy

import kotlinx.coroutines.runBlocking
import org.jameshpark.banksy.extractor.CsvExtractor
import org.jameshpark.banksy.loader.DbLoader
import org.jameshpark.banksy.transformer.DefaultTransformer
import java.time.Instant

fun main() = runBlocking {
    val extractor = CsvExtractor()
    val transformer = DefaultTransformer()
    val loader = DbLoader.fromUrl("jdbc:sqlite:database.db")

    val rows = extractor.extractTransactionData("transactions")
    val transactions = transformer.transform(rows)

    loader.initializeDatabase()
    loader.saveTransactions(transactions)

    val fileName = "export_${Instant.now().epochSecond}.csv"
    loader.exportToCsv("exports/$fileName")
}
