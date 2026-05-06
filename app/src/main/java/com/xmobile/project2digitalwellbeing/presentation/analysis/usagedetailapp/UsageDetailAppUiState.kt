package com.xmobile.project2digitalwellbeing.presentation.analysis.usagedetailapp

import android.graphics.drawable.Drawable

data class UsageDetailAppUiState(
    val appName: String = "",
    val packageName: String = "",
    val appIcon: Drawable? = null,
    val todayTotalFormatted: String = "0m",
    val todayVsYesterdayPercent: Int = 0,
    val weekLineChartData: List<Float> = emptyList(),
    val mostActivePeriod: String = "N/A",
    val avgSessionFormatted: String = "0m",
    val peakUsageLabel: String = "N/A",
    val todayHourlyBarChartData: List<Float> = emptyList(),
    val totalSessionsToday: Int = 0,
    val longestSessionFormatted: String = "0m",
    val shortestSessionFormatted: String = "0m",
    val timeStartLabel: String = "00:00",
    val timeMidLabel: String = "12:00",
    val timeEndLabel: String = "23:59",
    val topTransitions: List<AppTransitionUiModel> = emptyList(),
    val insightSummary: String = "Usage metrics calculated. Keep monitoring your digital well-being.",
    val tipSummary: String = "Monitor your screen time for better digital balance."
)

data class AppTransitionUiModel(
    val fromPackage: String,
    val fromAppName: String,
    val fromAppIcon: Drawable?,
    val toPackage: String,
    val toAppName: String,
    val toAppIcon: Drawable?,
    val count: Int
)
