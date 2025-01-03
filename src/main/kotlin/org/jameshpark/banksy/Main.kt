package org.jameshpark.banksy

import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = Banksy()
    .subcommands(EtlCommand(), ExportCommand())
    .main(args)
