package org.jameshpark.banksy

import kotlinx.coroutines.runBlocking
import org.jameshpark.banksy.extractor.CsvExtractor
import org.jameshpark.banksy.loader.DbLoader
import org.jameshpark.banksy.transformer.DefaultTransformer

fun main() = runBlocking {
    val csvExtractor = CsvExtractor()
    val transformer = DefaultTransformer()
    val loader = DbLoader.fromUrl("jdbc:sqlite:database.db")

    val rows = csvExtractor.extractTransactionData("transactions")
    val transactions = transformer.transform(rows)
//    CsvLoader().saveTransactions(transactions)

    loader.initializeDatabase()
    loader.saveTransactions(transactions)
}
