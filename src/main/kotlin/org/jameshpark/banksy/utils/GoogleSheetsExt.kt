package org.jameshpark.banksy.utils

import com.google.api.client.auth.oauth2.Credential
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jameshpark.banksy.models.GoogleSheetsSink
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader

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

fun sheetsServiceFromCredentials(credentialsFilePath: String): Sheets {
    val clientSecrets = loadCredentials(credentialsFilePath)
    val credential = getUserAuthorization(clientSecrets)

    return Sheets.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance(),
        credential
    )
        .setApplicationName("Banksy Kotlin App")
        .build()
}

private fun loadCredentials(credentialsFilePath: String): GoogleClientSecrets {
    val jsonFactory = GsonFactory.getDefaultInstance()
    val credentials: InputStream = GoogleSheetsSink::class.java.getResourceAsStream(credentialsFilePath)
        ?: throw FileNotFoundException("Resource not found: $credentialsFilePath")
    return GoogleClientSecrets.load(jsonFactory, InputStreamReader(credentials))
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
        .setDataStoreFactory(FileDataStoreFactory(File("tokens")))
        .setAccessType("offline")
        .build()
    val receiver = LocalServerReceiver.Builder().setPort(8888).build()
    return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
}
