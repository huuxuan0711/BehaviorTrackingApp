package com.xmobile.project2digitalwellbeing.data.tracking.source.system

import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppUsageEvent

interface UsageStatsDataSource {
    suspend fun getUsageEvents(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): List<AppUsageEvent>
}
