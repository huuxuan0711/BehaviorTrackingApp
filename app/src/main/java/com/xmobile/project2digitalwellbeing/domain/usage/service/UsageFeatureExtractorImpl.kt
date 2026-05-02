package com.xmobile.project2digitalwellbeing.domain.usage.service

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.model.EnrichedSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.SessionLengthDistribution
import com.xmobile.project2digitalwellbeing.domain.usage.model.TopCategoryFeature
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatureTopApp
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatures
import javax.inject.Inject

class UsageFeatureExtractorImpl @Inject constructor() : UsageFeatureExtractor {

    override fun extractFeatures(
        sessions: List<EnrichedSession>,
        preferences: UsageAnalysisPreferences
    ): UsageFeatures {
        val orderedSessions = sessions.sortedBy { it.session.startTimeMillis }
        val totalSessionCount = orderedSessions.size
        val totalScreenTimeMillis = orderedSessions.sumOf { it.session.durationMillis }
        val longestSessionMillis = orderedSessions.maxOfOrNull { it.session.durationMillis } ?: 0L
        val averageSessionLengthMillis = if (totalSessionCount == 0) {
            0L
        } else {
            totalScreenTimeMillis / totalSessionCount
        }

        val lateNightSessions = orderedSessions.filter { it.isLateNight }
        val lateNightUsageMillis = lateNightSessions.sumOf { it.session.durationMillis }
        val lateNightSessionCount = lateNightSessions.size
        val lateNightUsageRatio = if (totalScreenTimeMillis == 0L) {
            0f
        } else {
            lateNightUsageMillis.toFloat() / totalScreenTimeMillis.toFloat()
        }
        val lateNightAverageSessionLengthMillis = if (lateNightSessionCount == 0) {
            0L
        } else {
            lateNightUsageMillis / lateNightSessionCount
        }

        val switchCount = calculateSwitchCount(orderedSessions)
        val averageSwitchIntervalMillis = calculateAverageSwitchIntervalMillis(orderedSessions)
        val switchesPerHour = if (totalScreenTimeMillis == 0L) {
            0f
        } else {
            switchCount.toFloat() / (totalScreenTimeMillis.toFloat() / MILLIS_PER_HOUR.toFloat())
        }

        val peakUsageHour = orderedSessions
            .groupBy { it.hourOfDay }
            .mapValues { (_, groupedSessions) -> groupedSessions.sumOf { it.session.durationMillis } }
            .maxByOrNull { it.value }
            ?.key

        return UsageFeatures(
            totalScreenTimeMillis = totalScreenTimeMillis,
            totalSessionCount = totalSessionCount,
            longestSessionMillis = longestSessionMillis,
            lateNightUsageMillis = lateNightUsageMillis,
            lateNightSessionCount = lateNightSessionCount,
            lateNightUsageRatio = lateNightUsageRatio,
            lateNightAverageSessionLengthMillis = lateNightAverageSessionLengthMillis,
            switchCount = switchCount,
            switchesPerHour = switchesPerHour,
            averageSessionLengthMillis = averageSessionLengthMillis,
            averageSwitchIntervalMillis = averageSwitchIntervalMillis,
            peakUsageHour = peakUsageHour,
            sessionLengthDistribution = buildSessionLengthDistribution(
                sessions = orderedSessions,
                longSessionThresholdMillis = preferences.longSessionThresholdMillis
            ),
            topAppsByDuration = buildTopAppsByDuration(orderedSessions),
            topAppsByLaunchCount = buildTopAppsByLaunchCount(orderedSessions),
            topCategoriesByDuration = buildTopCategoriesByDuration(orderedSessions),
            lateNightTopApps = buildTopAppsByDuration(lateNightSessions)
        )
    }

    private fun calculateSwitchCount(sessions: List<EnrichedSession>): Int {
        if (sessions.size < 2) {
            return 0
        }

        return sessions.zipWithNext().count { (previous, next) ->
            previous.session.packageName != next.session.packageName
        }
    }

    private fun calculateAverageSwitchIntervalMillis(sessions: List<EnrichedSession>): Long {
        if (sessions.size < 2) {
            return 0L
        }

        val switchIntervals = sessions.zipWithNext()
            .filter { (previous, next) -> previous.session.packageName != next.session.packageName }
            .map { (previous, next) ->
                (next.session.startTimeMillis - previous.session.endTimeMillis).coerceAtLeast(0L)
            }

        if (switchIntervals.isEmpty()) {
            return 0L
        }

        return switchIntervals.sum() / switchIntervals.size
    }

    private fun buildSessionLengthDistribution(
        sessions: List<EnrichedSession>,
        longSessionThresholdMillis: Long
    ): SessionLengthDistribution {
        val totalCount = sessions.size
        val normalizedLongSessionThresholdMillis = longSessionThresholdMillis
            .coerceAtLeast(MIN_LONG_SESSION_THRESHOLD_MILLIS)
        val shortSessionCount = sessions.count { it.session.durationMillis < SHORT_SESSION_THRESHOLD_MILLIS }
        val mediumSessionCount = sessions.count {
            it.session.durationMillis in SHORT_SESSION_THRESHOLD_MILLIS..<normalizedLongSessionThresholdMillis
        }
        val longSessionCount = sessions.count { it.session.durationMillis >= normalizedLongSessionThresholdMillis }

        return SessionLengthDistribution(
            shortSessionCount = shortSessionCount,
            mediumSessionCount = mediumSessionCount,
            longSessionCount = longSessionCount,
            shortSessionRatio = shortSessionCount.toRatio(totalCount),
            mediumSessionRatio = mediumSessionCount.toRatio(totalCount),
            longSessionRatio = longSessionCount.toRatio(totalCount)
        )
    }

    private fun buildTopAppsByDuration(sessions: List<EnrichedSession>): List<UsageFeatureTopApp> {
        return sessions
            .groupBy { it.session.packageName }
            .map { (_, groupedSessions) -> groupedSessions.toTopAppFeature() }
            .sortedWith(
                compareByDescending<UsageFeatureTopApp> { it.totalTimeMillis }
                    .thenByDescending { it.launchCount }
                    .thenBy { it.packageName }
            )
    }

    private fun buildTopAppsByLaunchCount(sessions: List<EnrichedSession>): List<UsageFeatureTopApp> {
        return sessions
            .groupBy { it.session.packageName }
            .map { (_, groupedSessions) -> groupedSessions.toTopAppFeature() }
            .sortedWith(
                compareByDescending<UsageFeatureTopApp> { it.launchCount }
                    .thenByDescending { it.totalTimeMillis }
                    .thenBy { it.packageName }
            )
    }

    private fun buildTopCategoriesByDuration(sessions: List<EnrichedSession>): List<TopCategoryFeature> {
        return sessions
            .groupBy { it.category }
            .map { (category, groupedSessions) ->
                TopCategoryFeature(
                    category = category,
                    totalTimeMillis = groupedSessions.sumOf { it.session.durationMillis },
                    sessionCount = groupedSessions.size
                )
            }
            .sortedWith(
                compareByDescending<TopCategoryFeature> { it.totalTimeMillis }
                    .thenBy { it.category.name }
            )
    }

    private fun List<EnrichedSession>.toTopAppFeature(): UsageFeatureTopApp {
        val firstSession = first()
        return UsageFeatureTopApp(
            packageName = firstSession.session.packageName,
            appName = firstSession.appName,
            category = firstSession.category,
            totalTimeMillis = sumOf { it.session.durationMillis },
            launchCount = size
        )
    }

    private fun Int.toRatio(totalCount: Int): Float {
        if (totalCount == 0) {
            return 0f
        }
        return toFloat() / totalCount.toFloat()
    }

    private companion object {
        private const val SHORT_SESSION_THRESHOLD_MILLIS = 2L * 60L * 1000L
        private const val MIN_LONG_SESSION_THRESHOLD_MILLIS = 10L * 60L * 1000L
        private const val MILLIS_PER_HOUR = 60L * 60L * 1000L
    }
}
