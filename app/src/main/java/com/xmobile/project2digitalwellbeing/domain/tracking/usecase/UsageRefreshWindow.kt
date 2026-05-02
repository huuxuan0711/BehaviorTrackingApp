package com.xmobile.project2digitalwellbeing.domain.tracking.usecase

data class UsageRefreshWindow(
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val refreshMode: RefreshMode
)

enum class RefreshMode {
    FULL,
    INCREMENTAL
}
