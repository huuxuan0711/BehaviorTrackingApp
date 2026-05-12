package com.xmobile.project2digitalwellbeing.presentation.analysis.usagedetailapp

data class UsageDetailAppUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val appName: String = "",
    val packageName: String = "",
    val todayTotalFormatted: String = "0m",
    val todayVsYesterdayPercent: Int = 0,
    val weekLineChartData: List<Float> = emptyList(),
    val mostActivePeriod: String = "",
    val avgSessionFormatted: String = "0m",
    val peakUsageLabel: String = "",
    val todayHourlyBarChartData: List<Float> = emptyList(),
    val totalSessionsToday: Int = 0,
    val longestSessionFormatted: String = "0m",
    val shortestSessionFormatted: String = "0m",
    val timeStartLabel: String = "00:00",
    val timeMidLabel: String = "12:00",
    val timeEndLabel: String = "23:59",
    val topTransitions: List<AppTransitionUiModel> = emptyList(),
    val insightSummary: String = "",
    val tipSummary: String = ""
)

data class AppTransitionUiModel(
    val fromPackage: String,
    val fromAppName: String,
    val toPackage: String,
    val toAppName: String,
    val count: Int
)
