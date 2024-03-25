package org.jameshpark.org.jameshpark.banksy.models

import org.jameshpark.banksy.Mapper
import org.jameshpark.banksy.models.Category
import org.jameshpark.banksy.models.TransactionType
import org.jameshpark.banksy.models.getCategory
import java.math.BigDecimal
import java.time.LocalDate

data class Transaction(
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
    val category: Category,
    val type: TransactionType
)

fun Map<String, String>.toTransaction(mapper: Mapper): Transaction {
    val date = LocalDate.parse(get(mapper.dateColumn)!!, mapper.dateTimeFormatter)
    val description = get(mapper.descriptionColumn)!!
    val amount = BigDecimal(get(mapper.amountColumn))

    return Transaction(
        date,
        description,
        amount,
        getCategory(description),
        mapper.determineTransactionType(amount)
    )
}
