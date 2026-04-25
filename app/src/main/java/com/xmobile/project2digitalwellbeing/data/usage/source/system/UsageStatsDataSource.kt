package com.xmobile.project2digitalwellbeing.data.usage.source.system

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageEvent

interface UsageStatsDataSource {
    suspend fun getUsageEvents(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): List<AppUsageEvent>
}
