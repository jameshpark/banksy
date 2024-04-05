package org.jameshpark.org.jameshpark.banksy

import kotlinx.coroutines.runBlocking
import org.jameshpark.banksy.Extractor
import org.jameshpark.banksy.ExtractorEager
import org.jameshpark.banksy.Transformer
import org.jameshpark.banksy.models.Category
import org.jameshpark.banksy.models.TransactionType
import org.jameshpark.banksy.models.categoryFrom
import java.io.File
import java.math.BigDecimal

fun main() = runBlocking {
    val transactions = Extractor.extractFromCsv("transactions")
    val spendingByCategory = Transformer.spendingByCategory(transactions)

    spendingByCategory.forEach { println(it) }

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
