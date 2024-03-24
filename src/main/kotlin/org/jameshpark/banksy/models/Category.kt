package org.jameshpark.banksy.models

enum class Category {
    MORTGAGE,
    UTILITY,
    CAR,
    GAS,
    PHONE,
    SUBSCRIPTIONS,
    GROCERIES,
    RESTAURANTS,
    COFFEE,
    OTHER,
}

val categories = mapOf(
    "MORTGAGE" to Category.MORTGAGE,
)