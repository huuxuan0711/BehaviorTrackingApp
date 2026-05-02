package com.xmobile.project2digitalwellbeing.domain.usage.service

import com.xmobile.project2digitalwellbeing.domain.usage.model.DailyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageTrend
import com.xmobile.project2digitalwellbeing.domain.usage.model.WeeklyUsage

interface UsageTrendAnalyzer {
    fun compareDaily(current: DailyUsage, previous: DailyUsage?): UsageTrend

    fun compareWeekly(current: WeeklyUsage, previous: WeeklyUsage?): UsageTrend
}
