package org.jameshpark.banksy.models


interface Sink

class CsvSink(val filePath: String, val includeHeader: Boolean = false) : Sink
