package org.jameshpark.banksy

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val rows = Extractor.extractTransactionData("transactions")
    val transactions = Transformer.transform(rows)
    Loader.loadTransactions(transactions)

//    val spendingByCategory = Transformer.spendingByCategory(transactions)

//    spendingByCategory.forEach { println(it) }

//    val transactions = ExtractorEager.extractFromCsv("transactions")
//    transactions.map { it.description }.toSet().forEach { println(it) }

//    val transactions = ExtractorEager.extractFromCsv("transactions")
//    val regexes = File("regex.txt").readLines()
//    val result = regexes.associateWith { regex ->
//        val re = regex.toRegex(RegexOption.IGNORE_CASE)
//        transactions.filter { re.containsMatchIn(it.description) }.map { it.description }.toSet()
//    }
//    println(result)

//    val regexes = File("regex.txt").readLines()
//    regexes.sorted().forEach { println(it) }
}
