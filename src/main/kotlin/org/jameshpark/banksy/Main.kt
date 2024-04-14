package org.jameshpark.banksy

import kotlinx.coroutines.runBlocking
import org.jameshpark.banksy.extractor.CsvExtractor
import org.jameshpark.banksy.loader.CsvLoader
import org.jameshpark.banksy.transformer.DefaultTransformer

fun main() = runBlocking {
    val rows = CsvExtractor().extractTransactionData("transactions")
    val transactions = DefaultTransformer().transform(rows)
    CsvLoader().saveTransactions(transactions)
}
