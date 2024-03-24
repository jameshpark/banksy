package org.jameshpark.org.jameshpark.banksy

import kotlinx.coroutines.runBlocking
import org.jameshpark.banksy.Extractor

fun main() = runBlocking {
    val transactions = Extractor.extractFromCsv("transactions")
    transactions.collect { println(it) }
}
