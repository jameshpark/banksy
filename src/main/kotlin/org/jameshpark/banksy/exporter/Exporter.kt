package org.jameshpark.banksy.exporter

import org.jameshpark.banksy.models.Sink

interface Exporter<T: Sink> {
    suspend fun export(sink: T, sinceId: Int)
}