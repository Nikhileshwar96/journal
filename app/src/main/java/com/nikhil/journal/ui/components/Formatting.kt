package com.nikhil.journal.ui.components

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val fullDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
private val mediumDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/** "Today", "Yesterday", or a full localized date such as "Friday, November 13, 2026". */
fun LocalDate.relativeLabel(today: LocalDate = LocalDate.now()): String = when (this) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> format(fullDate)
}

fun LocalDate.mediumLabel(): String = format(mediumDate)

/** Single-line preview of body text with whitespace collapsed. */
fun String.preview(maxChars: Int = 160): String {
    val collapsed = trim().replace(Regex("\\s+"), " ")
    return if (collapsed.length <= maxChars) collapsed else collapsed.take(maxChars).trimEnd() + "…"
}
