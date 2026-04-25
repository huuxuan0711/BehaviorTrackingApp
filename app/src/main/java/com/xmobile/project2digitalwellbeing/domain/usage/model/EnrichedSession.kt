package com.xmobile.project2digitalwellbeing.domain.usage.model

data class EnrichedSession(
    val session: AppSession,
    val appName: String?,
    val category: AppCategory,
    val hourOfDay: Int,
    val isLateNight: Boolean
)
