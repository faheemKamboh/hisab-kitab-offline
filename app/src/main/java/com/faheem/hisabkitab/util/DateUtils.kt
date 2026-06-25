package com.faheem.hisabkitab.util

import com.faheem.hisabkitab.domain.DateRangePreset
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object DateUtils {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
    private val shortDateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM, hh:mm a")

    fun nowMillis(): Long = System.currentTimeMillis()

    fun startOfDayMillis(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun endExclusiveOfDayMillis(date: LocalDate): Long = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun millisToLocalDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun withCurrentTime(dateMillis: Long): Long {
        val selectedDate = millisToLocalDate(dateMillis)
        val nowTime = LocalDateTime.now(zone).toLocalTime()
        return selectedDate.atTime(nowTime).atZone(zone).toInstant().toEpochMilli()
    }

    fun rangeFor(preset: DateRangePreset): Pair<Long, Long> {
        val today = LocalDate.now(zone)
        val start = when (preset) {
            DateRangePreset.TODAY -> today
            DateRangePreset.WEEK -> today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            DateRangePreset.MONTH -> today.withDayOfMonth(1)
            DateRangePreset.YEAR -> today.withDayOfYear(1)
            DateRangePreset.ALL -> LocalDate.of(1970, 1, 1)
        }
        val end = when (preset) {
            DateRangePreset.TODAY -> today.plusDays(1)
            DateRangePreset.WEEK -> start.plusWeeks(1)
            DateRangePreset.MONTH -> start.plusMonths(1)
            DateRangePreset.YEAR -> start.plusYears(1)
            DateRangePreset.ALL -> LocalDate.of(2999, 12, 31)
        }
        return startOfDayMillis(start) to startOfDayMillis(end)
    }

    fun formatDate(millis: Long): String = Instant.ofEpochMilli(millis).atZone(zone).format(dateFormatter)

    fun formatDateTime(millis: Long): String = Instant.ofEpochMilli(millis).atZone(zone).format(dateTimeFormatter)

    fun formatShortDateTime(millis: Long): String = Instant.ofEpochMilli(millis).atZone(zone).format(shortDateTimeFormatter)
}
