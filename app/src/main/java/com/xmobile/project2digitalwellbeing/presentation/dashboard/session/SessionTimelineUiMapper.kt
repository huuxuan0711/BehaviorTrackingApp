package com.xmobile.project2digitalwellbeing.presentation.dashboard.session

import android.content.Context
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.EnrichedSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import com.xmobile.project2digitalwellbeing.helper.UsageFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class SessionTimelineUiMapper @Inject constructor() {

    fun toDateLabel(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault()))
    }

    fun toTimelineItems(
        context: Context,
        sessions: List<EnrichedSession>,
        zoneId: ZoneId,
        selectedDate: LocalDate,
        lateNightStartHour: Int
    ): List<SessionTimelineItemUiModel> {
        val mergedSessions = sessions.mergeAdjacentSameAppSessions(
            zoneId = zoneId,
            selectedDate = selectedDate,
            lateNightStartHour = lateNightStartHour
        )
        val maxDurationMillis = mergedSessions.maxOfOrNull { it.session.durationMillis } ?: 0L

        return mergedSessions.mapIndexed { index, session ->
            session.toTimelineItem(
                context = context,
                previousSession = mergedSessions.getOrNull(index - 1),
                maxDurationMillis = maxDurationMillis,
                zoneId = zoneId,
                selectedDate = selectedDate,
                lateNightStartHour = lateNightStartHour
            )
        }
    }

    private fun List<EnrichedSession>.mergeAdjacentSameAppSessions(
        zoneId: ZoneId,
        selectedDate: LocalDate,
        lateNightStartHour: Int
    ): List<EnrichedSession> {
        if (isEmpty()) return emptyList()

        val mergedSessions = mutableListOf<EnrichedSession>()
        for (session in this) {
            val previous = mergedSessions.lastOrNull()
            if (previous != null && previous.canMergeWith(
                    next = session,
                    zoneId = zoneId,
                    selectedDate = selectedDate,
                    lateNightStartHour = lateNightStartHour
                )
            ) {
                mergedSessions[mergedSessions.lastIndex] = previous.mergeWith(session)
            } else {
                mergedSessions += session
            }
        }
        return mergedSessions
    }

    private fun EnrichedSession.canMergeWith(
        next: EnrichedSession,
        zoneId: ZoneId,
        selectedDate: LocalDate,
        lateNightStartHour: Int
    ): Boolean {
        val gapMillis = next.session.startTimeMillis - session.endTimeMillis
        return session.packageName == next.session.packageName &&
            gapMillis in 0..MERGE_ADJACENT_SESSION_GAP_MILLIS &&
            session.startTimeMillis.toPeriodLabel(zoneId, selectedDate, lateNightStartHour) ==
            next.session.startTimeMillis.toPeriodLabel(zoneId, selectedDate, lateNightStartHour)
    }

    private fun EnrichedSession.mergeWith(next: EnrichedSession): EnrichedSession {
        val mergedStartTimeMillis = minOf(session.startTimeMillis, next.session.startTimeMillis)
        val mergedEndTimeMillis = maxOf(session.endTimeMillis, next.session.endTimeMillis)
        return copy(
            session = AppSession(
                packageName = session.packageName,
                startTimeMillis = mergedStartTimeMillis,
                endTimeMillis = mergedEndTimeMillis,
                durationMillis = session.durationMillis + next.session.durationMillis
            )
        )
    }

    private fun EnrichedSession.toTimelineItem(
        context: Context,
        previousSession: EnrichedSession?,
        maxDurationMillis: Long,
        zoneId: ZoneId,
        selectedDate: LocalDate,
        lateNightStartHour: Int
    ): SessionTimelineItemUiModel {
        return SessionTimelineItemUiModel(
            packageName = session.packageName,
            appName = appName ?: session.packageName,
            periodLabel = session.startTimeMillis.toPeriodLabel(
                zoneId = zoneId,
                selectedDate = selectedDate,
                lateNightStartHour = lateNightStartHour
            ),
            timeRangeText = UsageFormatter.formatTimeRange(session.startTimeMillis, session.endTimeMillis, zoneId),
            durationText = UsageFormatter.formatDurationVerbose(context, session.durationMillis),
            durationMillis = session.durationMillis,
            progressRatio = if (maxDurationMillis <= 0L) 0f else {
                session.durationMillis.toFloat() / maxDurationMillis.toFloat()
            },
            transitionLabel = previousSession
                ?.takeIf { it.session.packageName != session.packageName }
                ?.let { appName ?: session.packageName },
            category = category
        )
    }

    private fun Long.toPeriodLabel(
        zoneId: ZoneId,
        selectedDate: LocalDate,
        lateNightStartHour: Int
    ): String {
        val localDateTime = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDateTime()
        val hour = localDateTime.hour
        val prefix = when (localDateTime.hour) {
            in 0 until UsageAnalysisPreferences.DEFAULT_LATE_NIGHT_END_HOUR -> "After midnight"
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            else -> if (hour >= lateNightStartHour) "Late night" else "Evening"
        }
        return if (localDateTime.toLocalDate() == selectedDate) {
            prefix
        } else {
            "$prefix (${toDateLabel(localDateTime.toLocalDate())})"
        }
    }

    private companion object {
        private const val MERGE_ADJACENT_SESSION_GAP_MILLIS = 2L * 60L * 1000L
    }
}
