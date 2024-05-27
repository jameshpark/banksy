package org.jameshpark.banksy.models


class GoogleSheetsSink(
    val spreadsheetId: String,
    val sheetName: String
) : Sink
