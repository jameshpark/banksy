package org.jameshpark.banksy.models

import org.jameshpark.banksy.Mapper
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.LocalDate

data class Transaction(
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
    val category: Category,
    val type: TransactionType,
    val originHash: String
) {
    fun toRow(): List<String> = listOf(
        date.toString(),
        description,
        amount.toString(),
        category.name,
        type.name,
        originHash
    )
}

suspend fun Map<String, String>.toTransaction(mapper: Mapper): Transaction {
    val date = LocalDate.parse(get(mapper.dateColumn)!!, mapper.dateTimeFormatter)
    val description = get(mapper.descriptionColumn)!!
    val amount = BigDecimal(get(mapper.amountColumn)).abs()
    val originHash = hash()

    return Transaction(
        date,
        description,
        amount,
        categoryFrom(description),
        mapper.determineTransactionType(amount),
        originHash
    )
}

/**
 * Calculates the hash value of a map by converting it to a sorted map,
 * mapping each key-value pair to a string representation, joining them
 * with a comma, and then calculating the hash of the resulting string.
 *
 * @return The hash value as a string.
 */
fun Map<String, String>.hash(): String {
    return toSortedMap().map { (k, v) ->
        "$k=$v"
    }.joinToString(",").hash()
}

/**
 * Compute the MD5 hash of this string and return the result as a hex string.
 */
fun String.hash(): String {
    // compute the MD5 hash of test
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(toByteArray())

    return digest.joinToString("") { "%02x".format(it) }
}
