package com.xmobile.project2digitalwellbeing.domain.usage.model

import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppSession

data class DailyUsage(
    val localDate: String,
    val timezoneId: String,
    val totalScreenTimeMillis: Long,
    val totalSessionCount: Int,
    val sessions: List<AppSession>
)
