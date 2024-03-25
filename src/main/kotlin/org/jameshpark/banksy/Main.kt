package org.jameshpark.org.jameshpark.banksy

import kotlinx.coroutines.runBlocking
import org.jameshpark.banksy.Extractor
import org.jameshpark.banksy.ExtractorEager
import org.jameshpark.banksy.models.Category
import org.jameshpark.banksy.models.TransactionType
import java.math.BigDecimal

fun main() = runBlocking {
    val transactions = Extractor.extractFromCsv("transactions")
    val spendingByCategory = mutableMapOf<Category, BigDecimal>()
    transactions.collect { transaction ->
        val amount = when (transaction.type) {
            TransactionType.DEBIT -> transaction.amount
            TransactionType.CREDIT -> -transaction.amount
        }

        val sum = spendingByCategory[transaction.category]?.let {
            it + amount
        } ?: amount

        spendingByCategory[transaction.category] = sum
    }

    spendingByCategory.forEach { println(it) }

//    val transactions = ExtractorEager.extractFromCsv("transactions")
//    transactions.map { it.description }.toSet().forEach { println(it) }
}
