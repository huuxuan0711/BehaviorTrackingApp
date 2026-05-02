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

        return sessions
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
}
