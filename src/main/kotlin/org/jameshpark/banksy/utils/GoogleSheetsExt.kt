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
import com.google.api.services.sheets.v4.model.ValueRange
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.StringReader
import java.util.*

private val logger = KotlinLogging.logger { }
private val LOCAL_TOKENS_DIR = File("tokens")

suspend fun Sheets.appendRows(spreadsheetId: String, sheetName: String, rows: List<List<Any>>) {
    val firstEmptyRow = getFirstEmptyRow(spreadsheetId, sheetName)
    val range = "$sheetName!A$firstEmptyRow"
    val body = ValueRange().setValues(rows)
    val request = spreadsheets().values()
        .append(spreadsheetId, range, body)
        .setValueInputOption("USER_ENTERED")

    withContext(Dispatchers.IO) {
        request.execute()
    }
}

suspend fun Sheets.getFirstEmptyRow(spreadsheetId: String, sheetName: String): Int {
    val request = spreadsheets().values()
        .get(spreadsheetId, "$sheetName!A:A")
    val values = withContext(Dispatchers.IO) {
        request.execute()
    }.getValues()

    return values.size + 1
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
