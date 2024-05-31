package org.jameshpark.banksy.models

import org.jameshpark.banksy.utils.require
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.pathString

class CsvSink(val filePath: String, val includeHeader: Boolean = false) : Sink {
    companion object {

        fun fromProperties(properties: Properties): CsvSink {
            val dirPath = properties.require("app.export.transactions.to-directory")
            val fileName = generateFilename()
            val path = Paths.get(dirPath, fileName).pathString
            return CsvSink(path)
        }

        private fun generateFilename(): String {
            val currentDateTime = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss")
            val formattedDateTime = currentDateTime.format(formatter)
            return "export_${formattedDateTime}.csv"
        }

    }
}