package com.xmobile.project2digitalwellbeing.domain.tracking.service

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppMetadata
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.EnrichedSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences

interface SessionEnricher {
    fun enrichSessions(
        sessions: List<AppSession>,
        timezoneId: String,
        appMetadataByPackage: Map<String, AppMetadata>,
        preferences: UsageAnalysisPreferences
    ): List<EnrichedSession>
}
