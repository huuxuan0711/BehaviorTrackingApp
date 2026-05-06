package com.xmobile.project2digitalwellbeing.presentation.analysis.usagepattern

data class UsagePatternDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val averageSessionText: String = "0m",
    val longestSessionText: String = "0m",
    val totalSessionText: String = "0",
    val switchCountText: String = "0",
    val averageSwitchIntervalText: String = "0m",
    val shortSessionText: String = "0 sessions",
    val mediumSessionText: String = "0 sessions",
    val longSessionText: String = "0 sessions",
    val shortSessionRatio: Float = 0f,
    val mediumSessionRatio: Float = 0f,
    val longSessionRatio: Float = 0f,
    val topApps: List<UsagePatternTopAppUiModel> = emptyList(),
    val insightText: String = DEFAULT_INSIGHT
) {
    companion object {
        const val DEFAULT_INSIGHT = "Usage pattern insight will appear after enough activity is captured today."
    }
}

data class UsagePatternTopAppUiModel(
    val packageName: String,
    val appName: String,
    val sessionCountText: String,
    val progressRatio: Float
)
