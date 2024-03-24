package org.jameshpark.org.jameshpark.banksy

import kotlinx.coroutines.runBlocking
import org.jameshpark.banksy.Extractor
import org.jameshpark.banksy.ExtractorEager

fun main() = runBlocking {
//    val transactions = Extractor.extractFromCsv("transactions")
//    transactions.collect { println(it) }
    val transactions = ExtractorEager.extractFromCsv("transactions")
    transactions.forEach { println(it) }
}
