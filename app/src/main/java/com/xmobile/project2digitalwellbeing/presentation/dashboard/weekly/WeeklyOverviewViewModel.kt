package com.xmobile.project2digitalwellbeing.presentation.dashboard.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataOutcome
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataParams
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageTrendDirection
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetWeeklyOverviewDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetWeeklyOverviewDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetWeeklyOverviewDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.WeeklyOverviewDataError
import com.xmobile.project2digitalwellbeing.helper.UsageFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WeeklyOverviewViewModel @Inject constructor(
    private val refreshUsageDataUseCase: RefreshUsageDataUseCase,
    private val getWeeklyOverviewDataUseCase: GetWeeklyOverviewDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyOverviewUiState())
    val uiState: StateFlow<WeeklyOverviewUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<WeeklyOverviewEffect>()
    val effects: SharedFlow<WeeklyOverviewEffect> = _effects.asSharedFlow()

    private var hasLoadedData = false

    fun onPermissionMissing() {
        viewModelScope.launch {
            _effects.emit(WeeklyOverviewEffect.OpenPermission)
        }
    }

    fun load(forceRefresh: Boolean, weekStartDate: LocalDate = _uiState.value.weekStartDate) {
        val timezoneId = ZoneId.systemDefault().id
        val zoneId = ZoneId.of(timezoneId)
        val currentWeekStart = LocalDate.now(zoneId)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val normalizedWeekStart = weekStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val anchorDate = normalizedWeekStart.plusDays(6)
        val nowMillis = anchorDate.atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli()
        val shouldRefreshCurrentWeek = normalizedWeekStart == currentWeekStart

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    weekStartDate = normalizedWeekStart,
                    errorMessage = null
                )
            }

            val refreshError = if (shouldRefreshCurrentWeek && (forceRefresh || !hasLoadedData)) {
                when (
                    val refreshOutcome = refreshUsageDataUseCase(
                        RefreshUsageDataParams(
                            nowMillis = System.currentTimeMillis(),
                            timezoneId = timezoneId,
                            forceFullRefresh = forceRefresh
                        )
                    )
                ) {
                    is RefreshUsageDataOutcome.Success -> null
                    is RefreshUsageDataOutcome.Failure -> refreshOutcome.error.toUserMessage()
                }
            } else {
                null
            }

            when (
                val outcome = getWeeklyOverviewDataUseCase(
                    GetWeeklyOverviewDataParams(
                        nowMillis = nowMillis,
                        timezoneId = timezoneId
                    )
                )
            ) {
                is GetWeeklyOverviewDataOutcome.Success -> {
                    hasLoadedData = true
                    val weeklyUsage = outcome.data.weeklyUsage
                    val dailyUsages = weeklyUsage.dailyUsages
                    val peakUsage = dailyUsages.maxOfOrNull { it.totalScreenTimeMillis } ?: 0L
                    val maxDay = dailyUsages.maxByOrNull { it.totalScreenTimeMillis }

                    _uiState.value = WeeklyOverviewUiState(
                        weekStartDate = normalizedWeekStart,
                        dateRangeLabel = UsageFormatter.formatDateRange(normalizedWeekStart, normalizedWeekStart.plusDays(6)),
                        averageDailyScreenTimeText = UsageFormatter.formatDuration(weeklyUsage.averageDailyScreenTimeMillis),
                        mostUsedDayText = if (maxDay == null || maxDay.totalScreenTimeMillis <= 0L) {
                            "No data yet"
                        } else {
                            "${UsageFormatter.formatShortDay(maxDay.localDate)} - ${UsageFormatter.formatDuration(maxDay.totalScreenTimeMillis)}"
                        },
                        totalScreenTimeText = UsageFormatter.formatDuration(weeklyUsage.totalScreenTimeMillis),
                        trendText = refreshError ?: outcome.data.trend.toFriendlySummary().takeIf { it.isNotBlank() }
                        ?: "No weekly trend yet. Patterns become clearer after a few active days.",
                        chartBars = dailyUsages.map { dailyUsage ->
                            WeeklyChartBarUiModel(
                                label = UsageFormatter.formatShortDay(dailyUsage.localDate),
                                durationMinutes = dailyUsage.totalScreenTimeMillis / 60000f,
                                isHighlighted = peakUsage > 0L && dailyUsage.totalScreenTimeMillis == peakUsage
                            )
                        },
                        topApps = outcome.data.topApps,
                        errorMessage = refreshError,
                        canNavigateNext = normalizedWeekStart.isBefore(currentWeekStart)
                    )
                }

                is GetWeeklyOverviewDataOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            dateRangeLabel = UsageFormatter.formatDateRange(normalizedWeekStart, normalizedWeekStart.plusDays(6)),
                            trendText = refreshError ?: outcome.error.toUserMessage(),
                            errorMessage = refreshError ?: outcome.error.toUserMessage(),
                            canNavigateNext = normalizedWeekStart.isBefore(currentWeekStart)
                        )
                    }
                }
            }
        }
    }

    fun showPreviousWeek() {
        load(forceRefresh = false, weekStartDate = _uiState.value.weekStartDate.minusWeeks(1))
    }

    fun showNextWeek() {
        if (_uiState.value.canNavigateNext) {
            load(forceRefresh = false, weekStartDate = _uiState.value.weekStartDate.plusWeeks(1))
        }
    }

    private fun com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageDataError.toUserMessage(): String {
        return when (this) {
            is com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageDataError.PermissionDenied ->
                "Usage access is required to refresh your weekly overview."

            is com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageDataError.InvalidTimeZone ->
                "Your device time zone could not be resolved."

            else -> "The latest weekly usage data could not be refreshed."
        }
    }

    private fun WeeklyOverviewDataError.toUserMessage(): String {
        return when (this) {
            is WeeklyOverviewDataError.InvalidTimeZone ->
                "Your device time zone could not be resolved."

            else -> "Weekly overview data is not available yet."
        }
    }

    private fun com.xmobile.project2digitalwellbeing.domain.usage.model.UsageTrend.toFriendlySummary(): String {
        val ratioPercent = (kotlin.math.abs(deltaRatio) * 100f).toInt()
        return when (direction) {
            UsageTrendDirection.UP -> "Your screen time increased by $ratioPercent% compared to last week."
            UsageTrendDirection.DOWN -> "Your screen time decreased by $ratioPercent% compared to last week."
            UsageTrendDirection.FLAT -> "Your screen time is stable compared to last week."
        }
    }
}
