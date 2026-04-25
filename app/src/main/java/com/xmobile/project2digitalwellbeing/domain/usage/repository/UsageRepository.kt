package com.xmobile.project2digitalwellbeing.domain.usage.repository

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppMetadata
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageEvent
import com.xmobile.project2digitalwellbeing.domain.usage.model.Insight
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageSyncState

interface UsageRepository {
    suspend fun getUsageEvents(startTimeMillis: Long, endTimeMillis: Long): List<AppUsageEvent>

    suspend fun getAppMetadata(packageNames: Set<String>): Map<String, AppMetadata>

    suspend fun getSessions(startTimeMillis: Long, endTimeMillis: Long): List<AppSession>

    suspend fun saveSessions(sessions: List<AppSession>)

    suspend fun deleteSessionsInRange(startTimeMillis: Long, endTimeMillis: Long)

    suspend fun getInsights(startTimeMillis: Long, endTimeMillis: Long): List<Insight>

    suspend fun saveInsights(
        insights: List<Insight>,
        windowStartMillis: Long,
        windowEndMillis: Long
    )

    suspend fun deleteInsightsInRange(startTimeMillis: Long, endTimeMillis: Long)

    suspend fun getSyncState(): UsageSyncState

    suspend fun saveSyncState(state: UsageSyncState)
}
