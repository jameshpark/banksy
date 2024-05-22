package org.jameshpark.banksy.transformer

import org.jameshpark.banksy.models.TransactionType
import java.math.BigDecimal
import java.time.format.DateTimeFormatter


enum class Mapper(
    val dateColumn: String,
    val descriptionColumn: String,
    val amountColumn: String,
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
) {
    AMEX_CREDIT_CARD("Date", "Description", "Amount") {
        override fun determineTransactionType(amount: BigDecimal): TransactionType =
            if (amount > BigDecimal.ZERO) TransactionType.DEBIT else TransactionType.CREDIT
    },

    CHASE_CREDIT_CARD("Post Date", "Description", "Amount") {
        override fun determineTransactionType(amount: BigDecimal): TransactionType =
            if (amount < BigDecimal.ZERO) TransactionType.DEBIT else TransactionType.CREDIT
    },

    CHASE_CHECKING("Posting Date", "Description", "Amount") {
        override fun determineTransactionType(amount: BigDecimal): TransactionType =
            if (amount < BigDecimal.ZERO) TransactionType.DEBIT else TransactionType.CREDIT
    };

    abstract fun determineTransactionType(amount: BigDecimal): TransactionType
}

val headersToMapper = mapOf(
    setOf(
        "Date",
        "Description",
        "Amount"
    ) to Mapper.AMEX_CREDIT_CARD,
    setOf(
        "Transaction Date",
        "Post Date",
        "Description",
        "Category",
        "Type",
        "Amount",
        "Memo"
    ) to Mapper.CHASE_CREDIT_CARD,
    setOf(
        "Details",
        "Posting Date",
        "Description",
        "Amount",
        "Type",
        "Balance",
        "Check or Slip #",
        ""
    ) to Mapper.CHASE_CHECKING
)
