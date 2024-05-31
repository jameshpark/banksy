package org.jameshpark.banksy.models

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.*
import org.jameshpark.banksy.utils.getFileAsStream
import org.jameshpark.banksy.utils.getResourceAsStream

data class Merchant(
    val name: String,
    val category: Category,
    val regex: String
) {

    private val compiledRegex = regex.toRegex(RegexOption.IGNORE_CASE)

    fun existsIn(description: String): Boolean = compiledRegex.containsMatchIn(description)

}

val merchants by lazy {
    val mapper = jacksonObjectMapper()
    val defaultMerchants: List<Merchant> = getResourceAsStream("merchants.json").use { mapper.readValue(it) }
    val localMerchants: List<Merchant>? = getFileAsStream("local.merchants.json")?.use { mapper.readValue(it) }

    localMerchants?.let {
        (defaultMerchants + it).associateBy { merchant -> merchant.name }.values.toList()
    } ?: defaultMerchants
}

suspend fun categoryFrom(description: String) = coroutineScope {
    merchants.map {
        async {
            if (it.existsIn(description)) {
                it.category
            } else {
                null
            }
        }
    }
        .awaitAll()
        .filterNotNull()
        .firstOrNull() ?: Category.UNCATEGORIZED
}
