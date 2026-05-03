package com.xmobile.project2digitalwellbeing.domain.tracking.service

import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppUsageEvent
import com.xmobile.project2digitalwellbeing.domain.tracking.model.UsageEventType
import javax.inject.Inject

class SessionBuilderImpl @Inject constructor() : SessionBuilder {

    override fun buildSessions(
        events: List<AppUsageEvent>,
        rangeStartMillis: Long,
        rangeEndMillis: Long,
        nowMillis: Long
    ): List<AppSession> {
        if (rangeStartMillis > rangeEndMillis) {
            return emptyList()
        }

        val normalizedEvents = events
            .asSequence()
            .filter { it.packageName.isNotBlank() }
            .filter { it.timestampMillis in rangeStartMillis..rangeEndMillis }
            .distinctBy { Triple(it.packageName, it.timestampMillis, it.type) }
            .sortedBy { it.timestampMillis }
            .toList()

        val sessions = mutableListOf<AppSession>()
        var activePackageName: String? = null
        var activeStartTimeMillis: Long? = null

        normalizedEvents.forEach { event ->
            when (event.type) {
                UsageEventType.FOREGROUND -> {
                    when (activePackageName) {
                        null -> {
                            activePackageName = event.packageName
                            activeStartTimeMillis = event.timestampMillis
                        }
                        event.packageName -> {
                            // Ignore noisy duplicate foreground events for the already active app.
                        }
                        else -> {
                            closeActiveSession(
                                sessions = sessions,
                                activePackageName = activePackageName,
                                activeStartTimeMillis = activeStartTimeMillis,
                                endTimeMillis = event.timestampMillis.coerceAtMost(rangeEndMillis)
                            )
                            activePackageName = event.packageName
                            activeStartTimeMillis = event.timestampMillis
                        }
                    }
                }

                UsageEventType.BACKGROUND -> {
                    if (activePackageName == event.packageName) {
                        closeActiveSession(
                            sessions = sessions,
                            activePackageName = activePackageName,
                            activeStartTimeMillis = activeStartTimeMillis,
                            endTimeMillis = event.timestampMillis.coerceAtMost(rangeEndMillis)
                        )
                        activePackageName = null
                        activeStartTimeMillis = null
                    }
                }
            }
        }

        if (activePackageName != null && activeStartTimeMillis != null) {
            closeActiveSession(
                sessions = sessions,
                activePackageName = activePackageName,
                activeStartTimeMillis = activeStartTimeMillis,
                endTimeMillis = minOf(nowMillis.coerceAtLeast(rangeStartMillis), rangeEndMillis)
            )
        }

        return sessions.mergeAdjacent()
    }

    private fun List<AppSession>.mergeAdjacent(toleranceMillis: Long = 1000L): List<AppSession> {
        if (size <= 1) return this
        val merged = mutableListOf<AppSession>()
        var current = this[0]

        for (i in 1 until size) {
            val next = this[i]
            if (current.packageName == next.packageName &&
                next.startTimeMillis <= current.endTimeMillis + toleranceMillis
            ) {
                val newEnd = maxOf(current.endTimeMillis, next.endTimeMillis)
                current = current.copy(
                    endTimeMillis = newEnd,
                    durationMillis = newEnd - current.startTimeMillis
                )
            } else {
                if (current.durationMillis >= MIN_SESSION_DURATION_MILLIS) {
                    merged.add(current)
                }
                current = next
            }
        }
        if (current.durationMillis >= MIN_SESSION_DURATION_MILLIS) {
            merged.add(current)
        }
        return merged
    }

    private fun closeActiveSession(
        sessions: MutableList<AppSession>,
        activePackageName: String?,
        activeStartTimeMillis: Long?,
        endTimeMillis: Long
    ) {
        if (activePackageName == null || activeStartTimeMillis == null) {
            return
        }

        val durationMillis = endTimeMillis - activeStartTimeMillis
        if (durationMillis <= 0L) {
            return
        }

        sessions += AppSession(
            packageName = activePackageName,
            startTimeMillis = activeStartTimeMillis,
            endTimeMillis = endTimeMillis,
            durationMillis = durationMillis
        )
    }

    private companion object {
        private const val MIN_SESSION_DURATION_MILLIS = 500L
    }
}
