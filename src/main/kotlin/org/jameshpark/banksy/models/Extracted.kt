package org.jameshpark.banksy.models

interface Extracted {
    suspend fun toTransaction(): Transaction?
}
