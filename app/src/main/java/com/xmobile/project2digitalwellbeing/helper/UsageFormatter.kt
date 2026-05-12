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

    private fun appLocale(context: Context): Locale {
        val configuration = context.resources.configuration
        return if (configuration.locales.isEmpty) Locale.getDefault() else configuration.locales[0]
    }

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
                    .withLocale(appLocale(context))
            )
        }
    }

    fun formatDateRange(context: Context, startDate: LocalDate, endDate: LocalDate): String {
        val locale = appLocale(context)
        val sameYear = startDate.year == endDate.year
        val startFormatter = DateTimeFormatter.ofPattern(
            if (sameYear) "MMM d" else "MMM d, yyyy",
            locale
        )
        val endFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", locale)
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

    fun formatTimeRange(context: Context, startTimeMillis: Long, endTimeMillis: Long, zoneId: ZoneId): String {
        val locale = appLocale(context)
        val formatter = DateTimeFormatter.ofPattern("EEE, HH:mm", locale)
        val start = Instant.ofEpochMilli(startTimeMillis).atZone(zoneId)
        val end = Instant.ofEpochMilli(endTimeMillis).atZone(zoneId)
        return "${start.format(formatter)} - ${end.format(DateTimeFormatter.ofPattern("HH:mm", locale))}"
    }
}
