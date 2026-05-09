package com.xmobile.project2digitalwellbeing.helper

import android.content.Context
import com.xmobile.project2digitalwellbeing.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object UsageFormatter {

    fun formatDuration(context: Context, millis: Long): String {
        val totalMinutes = millis / (60L * 1000L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0L && minutes > 0L -> context.getString(R.string.auto_duration_hm, hours.toInt(), minutes.toInt())
            hours > 0L -> context.getString(R.string.auto_duration_h, hours.toInt())
            else -> context.getString(R.string.auto_duration_m, minutes.toInt())
        }
    }

    fun formatDurationVerbose(context: Context, millis: Long): String {
        val totalMinutes = millis / (60L * 1000L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0L && minutes > 0L -> context.getString(R.string.auto_duration_hm, hours.toInt(), minutes.toInt())
            hours > 0L -> context.getString(R.string.auto_duration_h, hours.toInt())
            else -> context.getString(R.string.auto_text_n_minutes_long, totalMinutes.toInt())
        }
    }

    fun formatFriendlyDate(context: Context, millis: Long, timezoneId: String, today: LocalDate): String {
        val localDate = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.of(timezoneId))
            .toLocalDate()
        return formatFriendlyDate(context, localDate, today)
    }

    fun formatFriendlyDate(context: Context, date: LocalDate, today: LocalDate): String {
        return if (date == today) {
            context.getString(R.string.auto_today)
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
