package com.xmobile.project2digitalwellbeing.presentation.analysis.usagepattern

data class UsagePatternDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val averageSessionText: String = "0m",
    val longestSessionText: String = "0m",
    val totalSessionText: String = "0",
    val switchCountText: String = "0",
    val averageSwitchIntervalText: String = "0m",
    val shortSessionText: String = "",
    val mediumSessionText: String = "",
    val longSessionText: String = "",
    val shortSessionRatio: Float = 0f,
    val mediumSessionRatio: Float = 0f,
    val longSessionRatio: Float = 0f,
    val topApps: List<UsagePatternTopAppUiModel> = emptyList(),
    val insightText: String = ""
)

data class UsagePatternTopAppUiModel(
    val packageName: String,
    val appName: String,
    val sessionCountText: String,
    val progressRatio: Float
)
