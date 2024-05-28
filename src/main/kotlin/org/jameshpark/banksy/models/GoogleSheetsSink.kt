package org.jameshpark.banksy.models

import org.jameshpark.banksy.utils.require
import java.util.*


class GoogleSheetsSink(
    val spreadsheetId: String,
    val spreadsheetName: String,
    val sheetName: String
) : Sink {
    companion object {
        fun fromProperties(properties: Properties) = GoogleSheetsSink(
            properties.require("google.sheets.spreadsheet-id"),
            properties.require("google.sheets.spreadsheet-name"),
            properties.require("google.sheets.sheet-name")
        )
    }
}
