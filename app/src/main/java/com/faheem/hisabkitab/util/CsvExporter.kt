package com.faheem.hisabkitab.util

import android.content.Context
import android.net.Uri
import com.faheem.hisabkitab.domain.LedgerTransaction

object CsvExporter {
    fun buildCsv(transactions: List<LedgerTransaction>): String {
        val header = listOf(
            "Date",
            "Type",
            "Category",
            "Payment Mode",
            "Amount PKR",
            "Balance Impact PKR",
            "Running Balance PKR",
            "Notes"
        )
        var running = 0L
        val rows = transactions.sortedWith(compareBy<LedgerTransaction> { it.occurredAt }.thenBy { it.id }).map { tx ->
            running += tx.impactMinor
            listOf(
                DateUtils.formatDateTime(tx.occurredAt),
                tx.kind.label,
                tx.category,
                tx.paymentMode,
                Money.plain(tx.amountMinor),
                Money.plain(tx.impactMinor),
                Money.plain(running),
                tx.notes
            )
        }
        return (listOf(header) + rows).joinToString("\n") { row -> row.joinToString(",") { it.escapeCsv() } }
    }

    fun writeCsv(context: Context, uri: Uri, transactions: List<LedgerTransaction>) {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(buildCsv(transactions).toByteArray(Charsets.UTF_8))
        } ?: error("Could not open selected file for writing.")
    }
}

private fun String.escapeCsv(): String {
    val needsQuotes = contains(',') || contains('"') || contains('\n') || contains('\r')
    val escaped = replace("\"", "\"\"")
    return if (needsQuotes) "\"$escaped\"" else escaped
}
