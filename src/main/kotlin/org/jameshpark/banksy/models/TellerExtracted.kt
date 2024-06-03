package org.jameshpark.banksy.models

data class TellerExtracted(private val tellerTransaction: TellerTransaction) : Extracted {
    override suspend fun toTransaction() = with(tellerTransaction) {
        Transaction(
            date = date,
            description = description,
            amount = amount,
            category = categoryFrom(description),
            type = if (amount < 0.toBigDecimal()) TransactionType.DEBIT else TransactionType.CREDIT,
            originHash = hash()
        )
    }

    private fun hash(): String = tellerTransaction.toString().hash()
}