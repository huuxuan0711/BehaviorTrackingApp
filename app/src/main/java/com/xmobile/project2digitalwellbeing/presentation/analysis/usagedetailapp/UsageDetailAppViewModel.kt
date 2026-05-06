package com.xmobile.project2digitalwellbeing.presentation.analysis.usagedetailapp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.domain.apps.usecase.GetAppMetadataUseCase
import com.xmobile.project2digitalwellbeing.domain.apps.usecase.ResolveAppNameUseCase
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class UsageDetailAppViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val usageRepository: UsageRepository,
    private val getAppMetadataUseCase: GetAppMetadataUseCase,
    private val resolveAppNameUseCase: ResolveAppNameUseCase
) : ViewModel() {

    private val packageName: String = savedStateHandle.get<String>("PACKAGE_NAME") ?: ""

    private val _uiState = MutableStateFlow(UsageDetailAppUiState(packageName = packageName))
    val uiState: StateFlow<UsageDetailAppUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        if (packageName.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Missing app package. Unable to load usage detail."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val zoneId = ZoneId.systemDefault()
                val todayDate = LocalDate.now(zoneId)
                val sevenDaysAgoDate = todayDate.minusDays(6)

                val nowMillis = System.currentTimeMillis()
                val past24hMillis = nowMillis - 24L * 60 * 60 * 1000
                val past48hMillis = nowMillis - 48L * 60 * 60 * 1000
                val sevenDaysAgoMillis = sevenDaysAgoDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

            // Read from database to ensure we have data even if the system has deleted old events.
            // The 8-day sync policy ensures this data is persisted in Room via background worker or dashboard refresh.
                val sessions = usageRepository.getSessions(sevenDaysAgoMillis, nowMillis)

            // Resolve App Info
                val appMetadataMap = getAppMetadataUseCase(setOf(packageName))
                val appName = appMetadataMap[packageName]?.appName ?: resolveAppNameUseCase(packageName)

            // Aggregate data
                var todayTotalMillis = 0L
                var yesterdayTotalMillis = 0L

                val dailyTotals = FloatArray(7) { 0f } // 0 = 6 days ago, ..., 6 = today
                val globalDailyTotals = FloatArray(7) { 0f }
                val hourlyTotals = FloatArray(24) { 0f }
                val todaySessions = mutableListOf<AppSession>()
                val allTodaySessionsGlobally = mutableListOf<AppSession>()
                val currentHour = Instant.ofEpochMilli(nowMillis).atZone(zoneId).hour

                for (session in sessions) {
                val sessionLocalDate = Instant.ofEpochMilli(session.startTimeMillis).atZone(zoneId).toLocalDate()
                val daysSinceSevenDaysAgo = java.time.temporal.ChronoUnit.DAYS.between(sevenDaysAgoDate, sessionLocalDate).toInt()

                if (daysSinceSevenDaysAgo in 0..6) {
                    globalDailyTotals[daysSinceSevenDaysAgo] += session.durationMillis / (60f * 1000f)
                    if (session.packageName == packageName) {
                        dailyTotals[daysSinceSevenDaysAgo] += session.durationMillis / (60f * 1000f)
                    }
                }

                if (session.packageName == packageName) {
                    if (session.startTimeMillis in past24hMillis..nowMillis) {
                        todayTotalMillis += session.durationMillis
                        todaySessions.add(session)
                        val startHour = Instant.ofEpochMilli(session.startTimeMillis).atZone(zoneId).hour
                        val hourIndex = if (startHour <= currentHour) {
                            23 - (currentHour - startHour)
                        } else {
                            23 - (currentHour + 24 - startHour)
                        }
                        if (hourIndex in 0..23) {
                            hourlyTotals[hourIndex] += session.durationMillis / (60f * 1000f)
                        }
                    } else if (session.startTimeMillis in past48hMillis until past24hMillis) {
                        yesterdayTotalMillis += session.durationMillis
                    }
                }

                if (session.startTimeMillis in past24hMillis..nowMillis) {
                    allTodaySessionsGlobally.add(session)
                }
                }

                val dailyPercentages = dailyTotals.mapIndexed { index, appTotal ->
                    if (globalDailyTotals[index] > 0) (appTotal / globalDailyTotals[index]) * 100f else 0f
                }

            // Stats
                val todayTotalFormatted = formatDuration(todayTotalMillis)
                val vsYesterdayPercent = if (yesterdayTotalMillis > 0) {
                    (((todayTotalMillis - yesterdayTotalMillis).toFloat() / yesterdayTotalMillis.toFloat()) * 100).toInt()
                } else 0

                var mostActiveHour = 0
                var peakUsage = 0f
                for (i in hourlyTotals.indices) {
                    if (hourlyTotals[i] > peakUsage) {
                        peakUsage = hourlyTotals[i]
                        mostActiveHour = i
                    }
                }

                val endHour = (mostActiveHour + 1)
                val mostActivePeriod = "${String.format("%02d:00", mostActiveHour)} - ${String.format("%02d:00", endHour)}"

                val peakUsageLabel = when (mostActiveHour) {
                    in 0..5 -> "Peak usage after midnight"
                    in 6..11 -> "Peak usage in morning"
                    in 12..16 -> "Peak usage in afternoon"
                    else -> "Peak usage at night"
                }

                val totalSessionsCount = todaySessions.size
                val avgSessionMillis = if (totalSessionsCount > 0) todayTotalMillis / totalSessionsCount else 0L
                val longestSessionMillis = todaySessions.maxOfOrNull { it.durationMillis } ?: 0L
                val shortestSessionMillis = todaySessions.minOfOrNull { it.durationMillis } ?: 0L

                val timeStartLabel = String.format("%02d:00", (currentHour + 1) % 24)
                val timeMidLabel = String.format("%02d:00", (currentHour + 13) % 24)
                val timeEndLabel = String.format("%02d:00", currentHour)

            // App Transitions
                allTodaySessionsGlobally.sortBy { it.startTimeMillis }
                val transitionCounts = mutableMapOf<Pair<String, String>, Int>()
                for (i in 0 until allTodaySessionsGlobally.size - 1) {
                val current = allTodaySessionsGlobally[i]
                val next = allTodaySessionsGlobally[i+1]

                // Gap less than 2 minutes to count as transition
                val gap = next.startTimeMillis - current.endTimeMillis
                if (gap in 0..2 * 60 * 1000L) {
                    if (current.packageName != next.packageName) {
                        if (current.packageName == packageName || next.packageName == packageName) {
                            val pair = Pair(current.packageName, next.packageName)
                            transitionCounts[pair] = transitionCounts.getOrDefault(pair, 0) + 1
                        }
                    }
                }
                }

                val allRequiredPackages = transitionCounts.keys.flatMap { listOf(it.first, it.second) }.toSet()
                val metaMap = getAppMetadataUseCase(allRequiredPackages)

                val topTransitionsSorted = transitionCounts.entries.sortedByDescending { it.value }
                    .take(3)
                    .map { (pair, count) ->
                        val fromPkg = pair.first
                        val toPkg = pair.second
                        AppTransitionUiModel(
                            fromPackage = fromPkg,
                            fromAppName = metaMap[fromPkg]?.appName ?: resolveAppNameUseCase(fromPkg),
                            toPackage = toPkg,
                            toAppName = metaMap[toPkg]?.appName ?: resolveAppNameUseCase(toPkg),
                            count = count
                        )
                    }

                val insightSummary = "You tend to use $appName mostly " + when (mostActiveHour) {
                    in 0..5 -> "late at night."
                    in 6..11 -> "in the morning."
                    in 12..16 -> "in the afternoon."
                    else -> "in the evening."
                }

                val tipSummary = when (mostActiveHour) {
                    in 0..5 -> "You used $appName most frequently after midnight. Consider setting app limits for better sleep quality."
                    in 6..11 -> "Morning usage detected. Ensure it does not distract you from your main daytime tasks."
                    in 12..16 -> "Afternoon usage is high. A short screen break might boost your focus."
                    else -> "Evening usage detected. Try winding down screen time an hour before bed."
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        appName = appName,
                        todayTotalFormatted = todayTotalFormatted,
                        todayVsYesterdayPercent = vsYesterdayPercent,
                        weekLineChartData = dailyPercentages,
                        mostActivePeriod = mostActivePeriod,
                        avgSessionFormatted = formatDurationVerbose(avgSessionMillis),
                        peakUsageLabel = peakUsageLabel,
                        todayHourlyBarChartData = hourlyTotals.toList(),
                        totalSessionsToday = totalSessionsCount,
                        longestSessionFormatted = formatDurationVerbose(longestSessionMillis),
                        shortestSessionFormatted = formatDurationVerbose(shortestSessionMillis),
                        timeStartLabel = timeStartLabel,
                        timeMidLabel = timeMidLabel,
                        timeEndLabel = timeEndLabel,
                        topTransitions = topTransitionsSorted,
                        insightSummary = insightSummary,
                        tipSummary = tipSummary
                    )
                }
            } catch (_: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to load app usage details right now."
                    )
                }
            }
        }
    }

    private fun formatDuration(millis: Long): String {
        val totalMinutes = millis / (60 * 1000)
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        if (hours > 0) return "${hours}h ${mins}m"
        return "${mins}m"
    }

    private fun formatDurationVerbose(millis: Long): String {
        val totalSeconds = millis / 1000
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60
        if (mins > 0) return "${mins}m ${secs}s"
        return "${secs}s"
    }
}
