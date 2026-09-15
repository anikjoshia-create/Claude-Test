package com.anik.later.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
private val dayYearFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

fun formatWhen(epochMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val moment = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val today = LocalDate.now(zone)
    val day = moment.toLocalDate()
    val prefix = when (day) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> if (day.year == today.year) day.format(dayFormat) else day.format(dayYearFormat)
    }
    return "$prefix at ${moment.format(timeFormat)}"
}

fun formatCountdown(epochMillis: Long): String {
    val delta = epochMillis - System.currentTimeMillis()
    val minutes = abs(delta) / 60_000
    val text = when {
        minutes < 1 -> "less than a minute"
        minutes < 60 -> "$minutes min"
        minutes < 60 * 24 -> "${minutes / 60} hr"
        else -> "${minutes / (60 * 24)} days"
    }
    return if (delta >= 0) "in $text" else "$text ago"
}
