package com.xmobile.project2digitalwellbeing.domain.tracking.service

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppMetadata
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.EnrichedSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class SessionEnricherImpl @Inject constructor() : SessionEnricher {

    override fun enrichSessions(
        sessions: List<AppSession>,
        timezoneId: String,
        appMetadataByPackage: Map<String, AppMetadata>,
        preferences: UsageAnalysisPreferences
    ): List<EnrichedSession> {
        val zoneId = ZoneId.of(timezoneId)
        return sessions.map { session ->
            val hourOfDay = Instant.ofEpochMilli(session.startTimeMillis).atZone(zoneId).hour
            val metadata = appMetadataByPackage[session.packageName]

            EnrichedSession(
                session = session,
                appName = metadata?.appName,
                category = metadata?.reportingCategory ?: AppCategory.UNKNOWN,
                hourOfDay = hourOfDay,
                isLateNight = hourOfDay >= preferences.lateNightStartHour ||
                    hourOfDay < UsageAnalysisPreferences.DEFAULT_LATE_NIGHT_END_HOUR
            )
        }
    }
}
