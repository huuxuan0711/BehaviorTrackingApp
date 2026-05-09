package com.xmobile.project2digitalwellbeing.domain.usage.model

import com.xmobile.project2digitalwellbeing.domain.insights.model.Insight
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppUsageEvent

data class UsageExport(
    val version: Int = 1,
    val exportDateMillis: Long = System.currentTimeMillis(),
    val sessions: List<AppSession>,
    val insights: List<Insight>,
    val rawEvents: List<AppUsageEvent>,
    val preferences: UsageAnalysisPreferences
)
