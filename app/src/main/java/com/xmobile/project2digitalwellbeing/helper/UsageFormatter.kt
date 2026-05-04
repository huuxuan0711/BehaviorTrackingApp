package com.xmobile.project2digitalwellbeing.helper

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object UsageFormatter {

    fun formatDuration(millis: Long): String {
        val totalMinutes = millis / (60L * 1000L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
            hours > 0L -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    fun formatDurationVerbose(millis: Long): String {
        val totalMinutes = millis / (60L * 1000L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
            hours > 0L -> "${hours}h"
            else -> "$totalMinutes minutes"
        }
    }

    fun formatFriendlyDate(millis: Long, timezoneId: String, today: LocalDate): String {
        val localDate = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.of(timezoneId))
            .toLocalDate()
        return formatFriendlyDate(localDate, today)
    }

    fun formatFriendlyDate(date: LocalDate, today: LocalDate): String {
        return if (date == today) {
            "Today"
        } else {
            date.format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                    .withLocale(Locale.getDefault())
            )
        }
    }

    fun formatDateRange(startDate: LocalDate, endDate: LocalDate): String {
        val sameYear = startDate.year == endDate.year
        val startFormatter = DateTimeFormatter.ofPattern(
            if (sameYear) "MMM d" else "MMM d, yyyy",
            Locale.getDefault()
        )
        val endFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
        return "${startDate.format(startFormatter)} - ${endDate.format(endFormatter)}"
    }

    fun formatShortDay(dateText: String): String {
        return try {
            LocalDate.parse(dateText).dayOfWeek.name
                .lowercase()
                .replaceFirstChar(Char::uppercaseChar)
                .take(3)
        } catch (e: Exception) {
            ""
        }
    }

    fun formatTimeRange(startTimeMillis: Long, endTimeMillis: Long, zoneId: ZoneId): String {
        val formatter = DateTimeFormatter.ofPattern("EEE, HH:mm", Locale.getDefault())
        val start = Instant.ofEpochMilli(startTimeMillis).atZone(zoneId)
        val end = Instant.ofEpochMilli(endTimeMillis).atZone(zoneId)
        return "${start.format(formatter)} - ${end.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))}"
    }
}