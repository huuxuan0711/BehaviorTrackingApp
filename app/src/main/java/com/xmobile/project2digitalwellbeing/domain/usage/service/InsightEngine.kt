package com.xmobile.project2digitalwellbeing.domain.usage.service

import com.xmobile.project2digitalwellbeing.domain.usage.model.DailyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.Insight
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatures

interface InsightEngine {
    fun generateInsights(
        features: UsageFeatures,
        dailyUsage: DailyUsage
    ): List<Insight>
}
