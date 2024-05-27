package org.jameshpark.banksy.exporter

import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jameshpark.banksy.database.Dao
import org.jameshpark.banksy.models.CsvSink
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class CsvExporter(
    private val dao: Dao,
    private val writer: CsvWriter = csvWriter()
) : Exporter<CsvSink> {
    override suspend fun export(sink: CsvSink, sinceId: Int) {
        val filePath = sink.filePath
        val includeHeader = sink.includeHeader

        logger.info { "Exporting to $filePath" }
        val output = File(filePath)

        // create new file
        withContext(Dispatchers.IO) {
            output.createNewFile()
            if (includeHeader) {
                output.writeText("date,description,amount,category,critical,type,originHash\n")
            }
        }

        val transactions = dao.getTransactionsNewerThanId(sinceId)

        val counter = AtomicInteger(0)
        writer.openAsync(output, append = true) {
            transactions.collect {
                withContext(Dispatchers.IO) {
                    writeRow(it.toCsvRow())
                }
                if (counter.incrementAndGet() % 10 == 0) {
                    logger.info { "Exported ${counter.get()} transactions to $filePath" }
                }
            }
        }
        logger.info { "Exported ${counter.get()} transactions to $filePath" }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}