package com.xmobile.project2digitalwellbeing.domain.usage.service

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppMetadata
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageStat
import com.xmobile.project2digitalwellbeing.domain.usage.model.CategoryUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.DailyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.HourlyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.WeeklyUsage

interface UsageAggregator {
    fun buildDailyUsage(
        sessions: List<AppSession>,
        timezoneId: String,
        localDate: String
    ): DailyUsage

    fun buildAppUsageStats(
        sessions: List<AppSession>,
        appMetadataByPackage: Map<String, AppMetadata> = emptyMap()
    ): List<AppUsageStat>

    fun buildHourlyUsage(
        sessions: List<AppSession>,
        timezoneId: String,
        localDate: String? = null
    ): List<HourlyUsage>

    fun buildWeeklyUsage(
        sessions: List<AppSession>,
        timezoneId: String,
        startLocalDate: String,
        endLocalDate: String
    ): WeeklyUsage

    fun buildCategoryUsage(
        sessions: List<AppSession>,
        appMetadataByPackage: Map<String, AppMetadata> = emptyMap()
    ): List<CategoryUsage>
}
