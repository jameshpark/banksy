package org.jameshpark.banksy.utils

import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jameshpark.banksy.models.TellerFeed
import java.io.FileInputStream
import java.io.FileNotFoundException


private val logger = KotlinLogging.logger { }

fun getResourceAsStream(path: String) =
    Thread.currentThread().contextClassLoader.getResourceAsStream(path)
        ?: throw FileNotFoundException("'$path' does not exist in classpath")

fun getFileAsStream(path: String) = try {
    FileInputStream(path)
} catch (e: FileNotFoundException) {
    logger.info { "'$path' does not exist in project, skipping." }
    null
}

fun tellerFeedsFromJson(path: String): List<TellerFeed> = getResourceAsStream(path).use { OBJECT_MAPPER.readValue(it) }
