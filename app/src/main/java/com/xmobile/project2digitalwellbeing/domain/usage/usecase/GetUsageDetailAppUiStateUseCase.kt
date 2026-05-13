package com.xmobile.project2digitalwellbeing.domain.usage.usecase

import android.content.Context
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.domain.apps.usecase.GetAppMetadataUseCase
import com.xmobile.project2digitalwellbeing.domain.apps.usecase.ResolveAppNameUseCase
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.HourlyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageAggregator
import com.xmobile.project2digitalwellbeing.presentation.analysis.usagedetailapp.AppTransitionUiModel
import com.xmobile.project2digitalwellbeing.presentation.analysis.usagedetailapp.UsageDetailAppUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetUsageDetailAppUiStateUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageRepository: UsageRepository,
    private val getAppMetadataUseCase: GetAppMetadataUseCase,
    private val resolveAppNameUseCase: ResolveAppNameUseCase,
    private val aggregator: UsageAggregator
) {

    suspend operator fun invoke(
        packageName: String,
        nowMillis: Long = System.currentTimeMillis()
    ): UsageDetailAppUiState {
        val zoneId = ZoneId.systemDefault()
        val todayDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val sevenDaysAgoDate = todayDate.minusDays(6)
        val past24hMillis = nowMillis - MILLIS_PER_DAY
        val past48hMillis = past24hMillis - MILLIS_PER_DAY
        val sevenDaysAgoMillis = sevenDaysAgoDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val sessions = usageRepository.getSessions(sevenDaysAgoMillis, nowMillis)
        val appName = resolveTargetAppName(packageName)
        val appSessions = sessions.filter { it.packageName == packageName }
        val todaySessions = mutableListOf<AppSession>()
        val allTodaySessions = mutableListOf<AppSession>()

        val todayTotalMillis = calculateWindowTotal(
            sessions = appSessions,
            windowStartMillis = past24hMillis,
            windowEndMillis = nowMillis,
            matchingSessions = todaySessions
        )
        val yesterdayTotalMillis = calculateWindowTotal(
            sessions = appSessions,
            windowStartMillis = past48hMillis,
            windowEndMillis = past24hMillis
        )

        sessions.forEach { session ->
            if (session.endTimeMillis > past24hMillis && session.startTimeMillis < nowMillis) {
                allTodaySessions.add(session)
            }
        }

        val dailyPercentages = buildDailyPercentages(
            sessions = sessions,
            packageName = packageName,
            zoneId = zoneId,
            sevenDaysAgoDate = sevenDaysAgoDate
        )
        val hourlyUsage = buildHourlyUsage(
            sessions = appSessions,
            zoneId = zoneId,
            past24hMillis = past24hMillis,
            nowMillis = nowMillis
        )
        val mostActiveHour = hourlyUsage
            .maxByOrNull { it.totalTimeMillis }
            ?.hourOfDay
            ?: 0
        val currentHour = Instant.ofEpochMilli(nowMillis).atZone(zoneId).hour

        return UsageDetailAppUiState(
            isLoading = false,
            errorMessage = null,
            appName = appName,
            packageName = packageName,
            todayTotalFormatted = formatDuration(todayTotalMillis),
            todayVsYesterdayPercent = calculatePercentDelta(todayTotalMillis, yesterdayTotalMillis),
            weekLineChartData = dailyPercentages,
            mostActivePeriod = formatHourRange(mostActiveHour),
            avgSessionFormatted = formatDurationVerbose(
                if (todaySessions.isNotEmpty()) todayTotalMillis / todaySessions.size else 0L
            ),
            peakUsageLabel = peakUsageLabel(mostActiveHour),
            todayHourlyBarChartData = hourlyUsage.map { it.totalTimeMillis / MILLIS_PER_MINUTE_FLOAT },
            totalSessionsToday = todaySessions.size,
            longestSessionFormatted = formatDurationVerbose(todaySessions.maxOfOrNull { it.durationMillis } ?: 0L),
            shortestSessionFormatted = formatDurationVerbose(todaySessions.minOfOrNull { it.durationMillis } ?: 0L),
            timeStartLabel = String.format("%02d:00", currentHour),
            timeMidLabel = String.format("%02d:00", (currentHour + 12) % HOURS_PER_DAY),
            timeEndLabel = String.format("%02d:00", currentHour),
            topTransitions = buildTopTransitions(allTodaySessions, packageName),
            insightSummary = buildInsightSummary(appName, mostActiveHour),
            tipSummary = buildTipSummary(appName, mostActiveHour)
        )
    }

    private suspend fun resolveTargetAppName(packageName: String): String {
        val appMetadataMap = getAppMetadataUseCase(setOf(packageName))
        return appMetadataMap[packageName]?.appName ?: resolveAppNameUseCase(packageName)
    }

    private fun calculateWindowTotal(
        sessions: List<AppSession>,
        windowStartMillis: Long,
        windowEndMillis: Long,
        matchingSessions: MutableList<AppSession>? = null
    ): Long {
        return sessions.sumOf { session ->
            if (session.endTimeMillis > windowStartMillis && session.startTimeMillis < windowEndMillis) {
                matchingSessions?.add(session)
                minOf(session.endTimeMillis, windowEndMillis) - maxOf(session.startTimeMillis, windowStartMillis)
            } else {
                0L
            }
        }
    }

    private fun buildDailyPercentages(
        sessions: List<AppSession>,
        packageName: String,
        zoneId: ZoneId,
        sevenDaysAgoDate: LocalDate
    ): List<Float> {
        val appDailyTotals = FloatArray(DAYS_IN_WEEK)
        val globalDailyTotals = FloatArray(DAYS_IN_WEEK)
        sessions.forEach { session ->
            val sessionDate = Instant.ofEpochMilli(session.startTimeMillis).atZone(zoneId).toLocalDate()
            val dayIndex = java.time.temporal.ChronoUnit.DAYS.between(sevenDaysAgoDate, sessionDate).toInt()
            if (dayIndex in 0 until DAYS_IN_WEEK) {
                val durationMinutes = session.durationMillis / MILLIS_PER_MINUTE_FLOAT
                globalDailyTotals[dayIndex] += durationMinutes
                if (session.packageName == packageName) {
                    appDailyTotals[dayIndex] += durationMinutes
                }
            }
        }
        return appDailyTotals.mapIndexed { index, appTotal ->
            if (globalDailyTotals[index] > 0f) (appTotal / globalDailyTotals[index]) * 100f else 0f
        }
    }

    private fun buildHourlyUsage(
        sessions: List<AppSession>,
        zoneId: ZoneId,
        past24hMillis: Long,
        nowMillis: Long
    ): List<HourlyUsage> {
        return aggregator.buildSlidingHourlyUsage(
            sessions = sessions,
            timezoneId = zoneId.id,
            windowStartMillis = past24hMillis,
            windowEndMillis = nowMillis
        )
    }

    private suspend fun buildTopTransitions(
        todaySessions: List<AppSession>,
        packageName: String
    ): List<AppTransitionUiModel> {
        val transitionCounts = mutableMapOf<Pair<String, String>, Int>()
        todaySessions.sortedBy { it.startTimeMillis }
            .zipWithNext()
            .forEach { (current, next) ->
                val gap = next.startTimeMillis - current.endTimeMillis
                val includesTarget = current.packageName == packageName || next.packageName == packageName
                if (gap in 0..MAX_TRANSITION_GAP_MILLIS && current.packageName != next.packageName && includesTarget) {
                    val pair = current.packageName to next.packageName
                    transitionCounts[pair] = transitionCounts.getOrDefault(pair, 0) + 1
                }
            }

        val requiredPackages = transitionCounts.keys.flatMap { listOf(it.first, it.second) }.toSet()
        val metadataMap = getAppMetadataUseCase(requiredPackages)
        return transitionCounts.entries
            .sortedByDescending { it.value }
            .take(MAX_TOP_TRANSITIONS)
            .map { (pair, count) ->
                AppTransitionUiModel(
                    fromPackage = pair.first,
                    fromAppName = metadataMap[pair.first]?.appName ?: resolveAppNameUseCase(pair.first),
                    toPackage = pair.second,
                    toAppName = metadataMap[pair.second]?.appName ?: resolveAppNameUseCase(pair.second),
                    count = count
                )
            }
    }

    private fun calculatePercentDelta(todayMillis: Long, yesterdayMillis: Long): Int {
        return if (yesterdayMillis > 0L) {
            (((todayMillis - yesterdayMillis).toFloat() / yesterdayMillis.toFloat()) * 100).toInt()
        } else {
            0
        }
    }

    private fun formatHourRange(startHour: Int): String {
        return "${String.format("%02d:00", startHour)} - ${String.format("%02d:00", startHour + 1)}"
    }

    private fun peakUsageLabel(hour: Int): String {
        return when (hour) {
            in 0..5 -> context.getString(R.string.auto_peak_usage_after_midnight)
            in 6..11 -> context.getString(R.string.auto_peak_usage_morning)
            in 12..16 -> context.getString(R.string.auto_peak_usage_afternoon)
            else -> context.getString(R.string.auto_peak_usage_night)
        }
    }

    private fun buildInsightSummary(appName: String, mostActiveHour: Int): String {
        return "You tend to use $appName mostly " + when (mostActiveHour) {
            in 0..5 -> context.getString(R.string.auto_usage_period_late_night)
            in 6..11 -> context.getString(R.string.auto_usage_period_morning)
            in 12..16 -> context.getString(R.string.auto_usage_period_afternoon)
            else -> context.getString(R.string.auto_usage_period_evening)
        }
    }

    private fun buildTipSummary(appName: String, mostActiveHour: Int): String {
        return when (mostActiveHour) {
            in 0..5 -> context.getString(R.string.auto_tip_after_midnight, appName)
            in 6..11 -> context.getString(R.string.auto_tip_morning_usage)
            in 12..16 -> context.getString(R.string.auto_tip_afternoon_usage)
            else -> context.getString(R.string.auto_tip_evening_usage)
        }
    }

    private fun formatDuration(millis: Long): String {
        val totalMinutes = millis / MILLIS_PER_MINUTE
        val hours = totalMinutes / MINUTES_PER_HOUR
        val mins = totalMinutes % MINUTES_PER_HOUR
        return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }

    private fun formatDurationVerbose(millis: Long): String {
        val totalSeconds = millis / 1000L
        val mins = totalSeconds / 60L
        val secs = totalSeconds % 60L
        return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
    }

    private companion object {
        const val DAYS_IN_WEEK = 7
        const val HOURS_PER_DAY = 24
        const val MINUTES_PER_HOUR = 60L
        const val MILLIS_PER_MINUTE = 60L * 1000L
        const val MILLIS_PER_MINUTE_FLOAT = 60f * 1000f
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
        const val MAX_TRANSITION_GAP_MILLIS = 2L * 60L * 1000L
        const val MAX_TOP_TRANSITIONS = 3
    }
}
