package org.jameshpark.banksy.exporter

import com.google.api.services.sheets.v4.Sheets
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jameshpark.banksy.database.Dao
import org.jameshpark.banksy.models.GoogleSheetsSink
import org.jameshpark.banksy.utils.appendRows


class GoogleSheetsExporter(
    private val dao: Dao,
    private val sheetsService: Sheets
) : Exporter<GoogleSheetsSink> {

    override suspend fun export(sink: GoogleSheetsSink, sinceId: Int) {
        val spreadsheetId = sink.spreadsheetId
        val sheetName = sink.sheetName
        val transactionRows = dao.getTransactionsNewerThanId(sinceId).map { it.toCsvRow() }.toList()

        if (transactionRows.isEmpty()) {
            logger.info { "No new transactions to append to Google Sheet." }
        } else {
            sheetsService.appendRows(spreadsheetId, sheetName, transactionRows, startRow = 1)
            logger.info { "Appended ${transactionRows.size} rows to ${sink.spreadsheetName}.$sheetName" }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

}
