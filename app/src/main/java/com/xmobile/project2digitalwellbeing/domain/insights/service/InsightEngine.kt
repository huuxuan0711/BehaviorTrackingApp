package com.xmobile.project2digitalwellbeing.domain.insights.service

import com.xmobile.project2digitalwellbeing.domain.usage.model.DailyUsage
import com.xmobile.project2digitalwellbeing.domain.insights.model.Insight
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatures

interface InsightEngine {
    fun generateInsights(
        features: UsageFeatures,
        dailyUsage: DailyUsage,
        preferences: UsageAnalysisPreferences
    ): List<Insight>
}
