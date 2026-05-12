package com.xmobile.project2digitalwellbeing.domain.reasoning.model

data class BehaviorBaseline(
    val averageScreenTimeMillis: Long,
    val averageLateNightUsageRatio: Float,
    val averageSwitchesPerHour: Float,
    val averageShortSessionRatio: Float,
    val averageWorkHourDistractionMillis: Long,
    val averageTopAppShare: Float
) {
    companion object {
        val EMPTY = BehaviorBaseline(
            averageScreenTimeMillis = 0L,
            averageLateNightUsageRatio = 0f,
            averageSwitchesPerHour = 0f,
            averageShortSessionRatio = 0f,
            averageWorkHourDistractionMillis = 0L,
            averageTopAppShare = 0f
        )
    }
}
