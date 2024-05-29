package org.jameshpark.banksy.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jameshpark.banksy.models.CsvFeed
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.*

private val logger = KotlinLogging.logger { }

fun loadProperties() = listOf(
    "application.properties" to ::getResourceAsStream,
    "local.properties" to ::getFileAsStream
).mapNotNull { (path, readPropertiesFrom) ->
    readPropertiesFrom(path)?.let {
        Properties().apply {
            load(it)
        }
    }
}.reduce { combined, properties ->
    combined.apply { putAll(properties) }
}

fun getResourceAsStream(path: String) =
    Thread.currentThread().contextClassLoader.getResourceAsStream(path)
        ?: throw FileNotFoundException("'$path' does not exist in classpath")

fun getFileAsStream(path: String) = try {
    FileInputStream(path)
} catch (e: FileNotFoundException) {
    logger.info { "'$path' does not exist in project, skipping." }
    null
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
