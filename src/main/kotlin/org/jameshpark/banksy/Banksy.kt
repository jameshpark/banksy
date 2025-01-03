package org.jameshpark.banksy

import com.fasterxml.jackson.databind.JsonMappingException
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.jameshpark.banksy.clients.TellerClient
import org.jameshpark.banksy.database.Dao
import org.jameshpark.banksy.database.DefaultDatabase
import org.jameshpark.banksy.database.NoOpDatabase
import org.jameshpark.banksy.exporter.CsvExporter
import org.jameshpark.banksy.exporter.GoogleSheetsExporter
import org.jameshpark.banksy.extractor.CsvExtractor
import org.jameshpark.banksy.extractor.TellerExtractor
import org.jameshpark.banksy.loader.DefaultLoader
import org.jameshpark.banksy.models.CsvFeed
import org.jameshpark.banksy.models.CsvSink
import org.jameshpark.banksy.models.GoogleSheetsSink
import org.jameshpark.banksy.models.TellerFeed
import org.jameshpark.banksy.transformer.DefaultTransformer
import org.jameshpark.banksy.utils.csvFeedsFromProperties
import org.jameshpark.banksy.utils.launchApp
import org.jameshpark.banksy.utils.require
import org.jameshpark.banksy.utils.sheetsServiceFromProperties
import org.jameshpark.banksy.utils.tellerFeedsFromJson
import java.io.FileNotFoundException

enum class ExtractionSource {
    CSV,
    TELLER,
}

enum class ExportDestination {
    CSV,
    GOOGLE_SHEET,
}

class Banksy : CliktCommand() {
    override fun run() {
        echo("Banksy. See --help for options.")
    }
}

class EtlCommand : CliktCommand() {
    private val extractionSource by option("--extract-from").enum<ExtractionSource>().required()
    private val exportDestination by option("--export-to").enum<ExportDestination>().required()
    private val dryRun by option("--dry-run").flag()

    override fun run() =
        launchApp {
            val db = DefaultDatabase.fromProperties(properties).register()
            val dao = Dao(db)

            val (extractor, feeds) =
                when (extractionSource) {
                    ExtractionSource.CSV -> {
                        val extractor = CsvExtractor(dao)
                        val feeds =
                            csvFeedsFromProperties(properties).also {
                                if (it.isEmpty()) {
                                    logger.warn { "No transaction csv files found. Quitting..." }
                                    return@launchApp
                                }
                            }
                        extractor to feeds
                    }

                    ExtractionSource.TELLER -> {
                        val tellerClient = TellerClient.fromProperties(properties).register()
                        val extractor = TellerExtractor(dao, tellerClient)
                        val feeds =
                            try {
                                tellerFeedsFromJson(properties.require("teller.feeds.json.path"))
                            } catch (e: FileNotFoundException) {
                                logger.warn { "No teller feeds found. Quitting..." }
                                return@launchApp
                            } catch (e: JsonMappingException) {
                                logger.error(e) { "Failed to load Teller feeds from json. Quitting..." }
                                return@launchApp
                            }
                        extractor to feeds
                    }
                }

            val transformer = DefaultTransformer()
            val loader =
                if (!dryRun) {
                    DefaultLoader(dao)
                } else {
                    logger.info { "This is a Dry Run. Nothing will be written or exported." }
                    DefaultLoader(Dao(NoOpDatabase(verbose = true)))
                }
            val transactionIdBeforeLoad = dao.getLatestTransactionId()

            supervisorScope {
                feeds.map { feed ->
                    launch {
                        val (rows, sourceName) =
                            when (extractor) {
                                is CsvExtractor -> {
                                    (feed as CsvFeed).let {
                                        extractor.extract(it) to it.file.name
                                    }
                                }

                                is TellerExtractor -> {
                                    (feed as TellerFeed).let {
                                        extractor.extract(it) to it.feedName.name
                                    }
                                }
                            }
                        val transactions = transformer.transform(rows, sourceName)
                        loader.saveTransactions(feed, transactions)
                    }
                }
            }

            when (exportDestination) {
                ExportDestination.CSV -> {
                    CsvExporter(dao).export(CsvSink.fromProperties(properties), transactionIdBeforeLoad)
                }

                ExportDestination.GOOGLE_SHEET -> {
                    GoogleSheetsExporter(
                        dao,
                        sheetsServiceFromProperties(properties),
                    ).export(GoogleSheetsSink.fromProperties(properties), transactionIdBeforeLoad)
                }
            }
        }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}

class ExportCommand : CliktCommand() {
    private val sinceId by option("--since-id").int().required()
    private val exportDestination by option("--export-to").enum<ExportDestination>().required()

    override fun run() =
        launchApp {
            val db = DefaultDatabase.fromProperties(properties).register()
            val dao = Dao(db)

            when (exportDestination) {
                ExportDestination.CSV -> {
                    CsvExporter(dao).export(CsvSink.fromProperties(properties), sinceId)
                }

                ExportDestination.GOOGLE_SHEET -> {
                    GoogleSheetsExporter(
                        dao,
                        sheetsServiceFromProperties(properties),
                    ).export(GoogleSheetsSink.fromProperties(properties), sinceId)
                }
            }
        }
}
