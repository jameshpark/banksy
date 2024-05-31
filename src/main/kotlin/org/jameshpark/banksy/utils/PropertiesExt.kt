package org.jameshpark.banksy.utils

import org.jameshpark.banksy.models.CsvFeed
import java.io.File
import java.util.*


fun loadProperties() = listOf(
    "application.properties" to ::getResourceAsStream,
    "local.properties" to ::getFileAsStream
).mapNotNull { (path, readPropertiesFrom) ->
    readPropertiesFrom(path)?.use {
        Properties().apply {
            load(it)
        }
    }
}.reduce { combined, properties ->
    combined.apply { putAll(properties) }
}

fun csvFeedsFromProperties(properties: Properties): List<CsvFeed> {
    val path = properties.require("app.import.transactions.from-directory")
    val directory = File(path)
    return if (directory.exists() && directory.isDirectory) {
        directory.walk().filter { it.isFile && it.extension.lowercase() == "csv" }.map { CsvFeed(it) }.toList()
    } else {
        emptyList()
    }
}

fun Properties.require(key: String): String =
    getProperty(key)?.let { value ->
        if (value.isBlank()) {
            throw IllegalArgumentException("Property '$key' has no value")
        } else {
            value
        }
    } ?: throw IllegalArgumentException("Property '$key' not found")
