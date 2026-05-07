package com.xmobile.project2digitalwellbeing.presentation.analysis.latenight

data class LateNightAnalysisUiState(
    val isLoading: Boolean = false,
    val totalScreenTime: String = "0m",
    val sessionCount: String = "0",
    val avgDuration: String = "0m",
    val hourlyUsage: List<Float> = emptyList(),
    val peakUsageLabel: String = "Peak usage between 11 PM - 1 AM",
    val topApps: List<LateNightAppUiModel> = emptyList(),
    val insightText: String = "No clear pattern yet.",
    val recommendationText: String = "Reducing phone use after midnight may improve sleep quality.",
    val errorMessage: String? = null
)

data class LateNightAppUiModel(
    val packageName: String,
    val appName: String,
    val durationText: String
)
