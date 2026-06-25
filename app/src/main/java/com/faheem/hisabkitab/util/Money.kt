package com.faheem.hisabkitab.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

object Money {
    private val formatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "PK")).apply {
        currency = Currency.getInstance("PKR")
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }

    fun parseToMinor(input: String): Long? {
        val normalized = input
            .replace(",", "")
            .replace("PKR", "", ignoreCase = true)
            .trim()
        if (normalized.isBlank()) return null
        val value = normalized.toBigDecimalOrNull() ?: return null
        if (value <= java.math.BigDecimal.ZERO) return null
        return value.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact()
    }

    fun format(minor: Long, showSign: Boolean = false): String {
        val amount = java.math.BigDecimal.valueOf(abs(minor), 2)
        val formatted = formatter.format(amount).replace("PKR", "PKR ")
        return when {
            showSign && minor > 0 -> "+$formatted"
            showSign && minor < 0 -> "-$formatted"
            minor < 0 -> "-$formatted"
            else -> formatted
        }
    }

    fun plain(minor: Long): String = java.math.BigDecimal.valueOf(minor, 2).toPlainString()
}
