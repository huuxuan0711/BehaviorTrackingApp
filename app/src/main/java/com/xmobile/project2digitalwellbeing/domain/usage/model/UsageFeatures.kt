package com.xmobile.project2digitalwellbeing.domain.usage.model

data class UsageFeatures(
    val totalScreenTimeMillis: Long,
    val totalSessionCount: Int,
    val longestSessionMillis: Long,
    val lateNightUsageMillis: Long,
    val lateNightSessionCount: Int,
    val lateNightUsageRatio: Float,
    val lateNightAverageSessionLengthMillis: Long,
    val switchCount: Int,
    val switchesPerHour: Float,
    val averageSessionLengthMillis: Long,
    val averageSwitchIntervalMillis: Long,
    val peakUsageHour: Int?,
    val sessionLengthDistribution: SessionLengthDistribution,
    val topAppsByDuration: List<UsageFeatureTopApp>,
    val topAppsByLaunchCount: List<UsageFeatureTopApp>,
    val topCategoriesByDuration: List<TopCategoryFeature>,
    val lateNightTopApps: List<UsageFeatureTopApp>
)
