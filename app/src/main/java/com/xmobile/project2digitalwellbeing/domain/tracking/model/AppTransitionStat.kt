package com.xmobile.project2digitalwellbeing.domain.tracking.model

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory

data class AppTransitionStat(
    val fromPackageName: String,
    val fromAppName: String?,
    val fromCategory: AppCategory,
    val toPackageName: String,
    val toAppName: String?,
    val toCategory: AppCategory,
    val transitionCount: Int,
    val averageIntervalMillis: Long,
    val totalIntervalMillis: Long,
    val lateNightTransitionCount: Int,
    val lastTransitionTimestampMillis: Long
)
