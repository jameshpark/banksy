package org.jameshpark.banksy.models

data class TellerExtracted(
    private val tellerTransaction: TellerTransaction,
    private val sourceFeed: FeedName
) :
    Extracted {
    override suspend fun toTransaction() = with(tellerTransaction) {
        Transaction(
            date = date,
            description = description,
            amount = amount.abs(),
            category = categoryFrom(description),
            type = when (sourceFeed) {
                FeedName.CHASE_CHECKING -> if (amount < 0.toBigDecimal()) TransactionType.DEBIT else TransactionType.CREDIT
                else -> if (amount < 0.toBigDecimal()) TransactionType.CREDIT else TransactionType.DEBIT
            },
            originHash = hash()
        )
    }

    private fun hash(): String = tellerTransaction.toString().hash()
}