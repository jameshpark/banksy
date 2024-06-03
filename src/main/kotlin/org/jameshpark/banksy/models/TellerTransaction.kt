package org.jameshpark.banksy.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate

data class TellerTransaction(
    val status: Status,
    val id: String,
    val amount: BigDecimal,
    val date: LocalDate,
    val description: String
)

enum class Status {
    @JsonProperty("posted")
    POSTED,

    @JsonProperty("pending")
    PENDING
}
