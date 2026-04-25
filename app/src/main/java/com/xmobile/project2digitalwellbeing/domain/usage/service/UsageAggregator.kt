package com.xmobile.project2digitalwellbeing.domain.usage.service

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageStat
import com.xmobile.project2digitalwellbeing.domain.usage.model.DailyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.HourlyUsage

interface UsageAggregator {
    fun buildDailyUsage(
        sessions: List<AppSession>,
        timezoneId: String,
        localDate: String
    ): DailyUsage

    fun buildAppUsageStats(sessions: List<AppSession>): List<AppUsageStat>

    fun buildHourlyUsage(sessions: List<AppSession>, timezoneId: String): List<HourlyUsage>
}
