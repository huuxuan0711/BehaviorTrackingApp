package com.xmobile.project2digitalwellbeing.domain.usage.model

data class AppSession(
    val packageName: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMillis: Long
)
