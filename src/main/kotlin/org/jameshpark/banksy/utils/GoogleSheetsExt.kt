package org.jameshpark.banksy.utils

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.auth.oauth2.TokenResponseException
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.DimensionRange
import com.google.api.services.sheets.v4.model.InsertDimensionRequest
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.ValueRange
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.StringReader
import java.util.*

private val logger = KotlinLogging.logger { }
private val LOCAL_TOKENS_DIR = File("tokens")

/**
 * Appends rows to a specified sheet in a Google Sheets spreadsheet.
 *
 * @param spreadsheetId The ID of the spreadsheet.
 * @param sheetName The name of the sheet.
 * @param rows The list of rows to append. Each row is represented as a list of values.
 * @param startRow The optional starting row index (0-based). If provided, rows will be inserted starting from this row.
 *                 If not provided, the method will find the first empty row in the sheet and start inserting rows from there.
 */
suspend fun Sheets.appendRows(spreadsheetId: String, sheetName: String, rows: List<List<Any>>, startRow: Int? = null) {
    val row = startRow?.also {
        insertRows(spreadsheetId, sheetName, it, rows.size)
    } ?: getFirstEmptyRow(spreadsheetId, sheetName)
    val range = "$sheetName!A$row"
    val body = ValueRange().setValues(rows)
    val request = spreadsheets().values()
        .append(spreadsheetId, range, body)
        .setValueInputOption("USER_ENTERED")

    withContext(Dispatchers.IO) {
        request.execute()
    }
}

/**
 * Inserts a specified number of rows into a sheet in a Google Sheets spreadsheet.
 *
 * @param spreadsheetId The ID of the spreadsheet.
 * @param sheetName The name of the sheet.
 * @param startRow The index of the starting row (0-based) where the insertion will begin.
 * @param numberOfRows The number of rows to insert into the sheet.
 *
 * @throws IllegalArgumentException if the sheet with the given name is not found in the spreadsheet.
 * @throws IOException if an error occurs while executing the insertion request.
 */
suspend fun Sheets.insertRows(spreadsheetId: String, sheetName: String, startRow: Int, numberOfRows: Int) {
    val request = Request().apply {
        insertDimension = InsertDimensionRequest().apply {
            range = DimensionRange().apply {
                sheetId = this@insertRows.getSheetId(spreadsheetId, sheetName)
                dimension = "ROWS"
                startIndex = startRow
                endIndex = startRow + numberOfRows
            }
            inheritFromBefore = false
        }
    }
    val batchUpdateSpreadsheetRequest = BatchUpdateSpreadsheetRequest().apply {
        requests = listOf(request)
    }
    val batchUpdateRequest = spreadsheets().batchUpdate(spreadsheetId, batchUpdateSpreadsheetRequest)

    withContext(Dispatchers.IO) {
        batchUpdateRequest.execute()
    }
}

fun sheetsServiceFromProperties(properties: Properties): Sheets {
    val secretJson = properties.require("google.client.secret.json")
    return sheetsServiceFromSecretJson(secretJson)
}

fun sheetsServiceFromSecretJson(secretJson: String): Sheets {
    val clientSecrets = loadGoogleClientSecrets(secretJson)

    return buildSheetsService(clientSecrets).let {
        try {
            it.spreadsheets().get("FAKE_SPREADSHEET_ID").execute()
            it
        } catch (e: TokenResponseException) {
            logger.warn { "Both access and refresh tokens have expired. Purging and reauthenticating.." }
            if (LOCAL_TOKENS_DIR.exists()) LOCAL_TOKENS_DIR.deleteRecursively()
            buildSheetsService(clientSecrets)

        } catch (t: Throwable) {
            // This means our request to Google was authorized and
            // couldn't find the fake spreadsheet. Yay!
            it
        }
    }
}

private suspend fun Sheets.getSheetId(spreadsheetId: String, sheetName: String): Int? {
    val spreadsheet = withContext(Dispatchers.IO) {
        spreadsheets().get(spreadsheetId).execute()
    }
    val sheet = spreadsheet.sheets.firstOrNull { it.properties.title == sheetName }
    return sheet?.properties?.sheetId
        ?: throw IllegalArgumentException("Sheet $sheetName not found in spreadsheetId $spreadsheetId")
}

private suspend fun Sheets.getFirstEmptyRow(spreadsheetId: String, sheetName: String): Int {
    val request = spreadsheets().values()
        .get(spreadsheetId, "$sheetName!A:A")
    val values = withContext(Dispatchers.IO) {
        request.execute()
    }.getValues()

    return values.size + 1
}

private fun buildSheetsService(clientSecrets: GoogleClientSecrets): Sheets =
    Sheets.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance(),
        getUserAuthorization(clientSecrets)
    )
        .setApplicationName("Banksy Kotlin App")
        .build()

private fun loadGoogleClientSecrets(credentialsJson: String): GoogleClientSecrets {
    val jsonFactory = GsonFactory.getDefaultInstance()
    return GoogleClientSecrets.load(jsonFactory, StringReader(credentialsJson))
}

private fun getUserAuthorization(clientSecrets: GoogleClientSecrets): Credential {
    val jsonFactory = GsonFactory.getDefaultInstance()
    val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    val flow = GoogleAuthorizationCodeFlow.Builder(
        httpTransport,
        jsonFactory,
        clientSecrets,
        listOf(SheetsScopes.SPREADSHEETS)
    )
        .setDataStoreFactory(FileDataStoreFactory(LOCAL_TOKENS_DIR))
        .setAccessType("offline")
        .build()
    val receiver = LocalServerReceiver.Builder().setPort(8888).build()
    return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
}
