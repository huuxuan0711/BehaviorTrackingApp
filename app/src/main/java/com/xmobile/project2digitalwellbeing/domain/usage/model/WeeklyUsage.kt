package com.xmobile.project2digitalwellbeing.domain.usage.model

data class WeeklyUsage(
    val startLocalDate: String,
    val endLocalDate: String,
    val timezoneId: String,
    val totalScreenTimeMillis: Long,
    val averageDailyScreenTimeMillis: Long,
    val mostUsedLocalDate: String?,
    val dailyUsages: List<DailyUsage>
)
