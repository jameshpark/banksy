package org.jameshpark.banksy.exporter

import com.github.doyaaaaaken.kotlincsv.client.CsvFileWriter
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import com.github.doyaaaaaken.kotlincsv.client.KotlinCsvExperimental
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
        val transactions = dao.getTransactionsNewerThanId(sinceId)

        var output: File? = null
        var csvFileWriter: CsvFileWriter? = null
        var counter: AtomicInteger? = null

        try {
            transactions.collect { transaction ->
                output = setOrGetFile(output, filePath, sink.includeHeader)
                csvFileWriter = setOrGetWriter(csvFileWriter, output!!)
                counter = setOrGetCounter(counter)

                withContext(Dispatchers.IO) {
                    csvFileWriter!!.writeRow(transaction.toCsvRow())
                }

                if (counter!!.incrementAndGet() % 10 == 0) {
                    logger.info { "Exported ${counter!!.get()} transactions to $filePath" }
                }
            }

            logger.info { "Exported ${counter!!.get()} transactions to $filePath" }
        } finally {
            csvFileWriter?.close()
        }

        if (output == null) {
            logger.info { "No new transactions to export to csv." }
        }
    }

    private suspend fun setOrGetFile(output: File?, filePath: String, includeHeader: Boolean) =
        output ?: withContext(Dispatchers.IO) {
            File(filePath).apply {
                parentFile.mkdirs()
                createNewFile()
                if (includeHeader) {
                    writeText(CSV_HEADER)
                }
            }
        }

    @OptIn(KotlinCsvExperimental::class)
    private fun setOrGetWriter(csvFileWriter: CsvFileWriter?, output: File) =
        csvFileWriter ?: writer.openAndGetRawWriter(output, append = true)

    private fun setOrGetCounter(counter: AtomicInteger?) = counter ?: AtomicInteger(0)

    companion object {
        private val logger = KotlinLogging.logger { }
        private const val CSV_HEADER = "date,description,amount,category,critical,type,originHash\n"
    }

}