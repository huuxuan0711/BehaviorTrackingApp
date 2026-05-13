package com.xmobile.project2digitalwellbeing.presentation.dashboard.weekly

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase.GetWeeklyOverviewExperienceOutcome
import com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase.GetWeeklyOverviewExperienceUseCase
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataOutcome
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataParams
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetWeeklyOverviewDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.WeeklyOverviewDataError
import com.xmobile.project2digitalwellbeing.helper.UsageFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
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
    application: Application,
    private val refreshUsageDataUseCase: RefreshUsageDataUseCase,
    private val getWeeklyOverviewExperienceUseCase: GetWeeklyOverviewExperienceUseCase
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()

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
        val historicalBackfillStartMillis = normalizedWeekStart
            .minusWeeks(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val historicalBackfillEndMillis = normalizedWeekStart
            .plusWeeks(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    weekStartDate = normalizedWeekStart,
                    isLoading = true,
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
            } else if (!shouldRefreshCurrentWeek) {
                when (
                    val refreshOutcome = refreshUsageDataUseCase(
                        RefreshUsageDataParams(
                            nowMillis = historicalBackfillEndMillis - 1L,
                            timezoneId = timezoneId,
                            requestedRangeStartMillis = historicalBackfillStartMillis,
                            requestedRangeEndMillis = historicalBackfillEndMillis,
                            updateSyncState = false
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
                val outcome = getWeeklyOverviewExperienceUseCase(
                    GetWeeklyOverviewDataParams(
                        nowMillis = nowMillis,
                        timezoneId = timezoneId
                    )
                )
            ) {
                is GetWeeklyOverviewExperienceOutcome.Success -> {
                    hasLoadedData = true
                    val weeklyUsage = outcome.data.weeklyData.weeklyUsage
                    val dailyUsages = weeklyUsage.dailyUsages
                    val peakUsage = dailyUsages.maxOfOrNull { it.totalScreenTimeMillis } ?: 0L
                    val maxDay = dailyUsages.maxByOrNull { it.totalScreenTimeMillis }

                    _uiState.value = WeeklyOverviewUiState(
                        weekStartDate = normalizedWeekStart,
                        dateRangeLabel = UsageFormatter.formatDateRange(context, normalizedWeekStart, normalizedWeekStart.plusDays(6)),
                        averageDailyScreenTimeText = UsageFormatter.formatDuration(context, weeklyUsage.averageDailyScreenTimeMillis),
                        mostUsedDayText = if (maxDay == null || maxDay.totalScreenTimeMillis <= 0L) {
                            context.getString(R.string.auto_no_usage_data)
                        } else {
                            "${UsageFormatter.formatShortDay(maxDay.localDate)} - ${UsageFormatter.formatDuration(context, maxDay.totalScreenTimeMillis)}"
                        },
                        totalScreenTimeText = UsageFormatter.formatDuration(context, weeklyUsage.totalScreenTimeMillis),
                        trendText = refreshError ?: outcome.data.insightSummaryText,
                        chartBars = dailyUsages.map { dailyUsage ->
                            WeeklyChartBarUiModel(
                                label = UsageFormatter.formatShortDay(dailyUsage.localDate),
                                durationMinutes = dailyUsage.totalScreenTimeMillis / 60000f,
                                isHighlighted = peakUsage > 0L && dailyUsage.totalScreenTimeMillis == peakUsage
                            )
                        },
                        topApps = outcome.data.weeklyData.topApps,
                        isLoading = false,
                        errorMessage = refreshError,
                        canNavigateNext = normalizedWeekStart.isBefore(currentWeekStart)
                    )
                }

                is GetWeeklyOverviewExperienceOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            dateRangeLabel = UsageFormatter.formatDateRange(context, normalizedWeekStart, normalizedWeekStart.plusDays(6)),
                            trendText = refreshError ?: outcome.error.toUserMessage(),
                            isLoading = false,
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
                context.getString(R.string.auto_error_permission_denied)

            is com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageDataError.InvalidTimeZone ->
                context.getString(R.string.auto_error_invalid_timezone)

            else -> context.getString(R.string.auto_error_refresh_failure)
        }
    }

    private fun WeeklyOverviewDataError.toUserMessage(): String {
        return when (this) {
            is WeeklyOverviewDataError.InvalidTimeZone ->
                context.getString(R.string.auto_error_invalid_timezone)

            else -> context.getString(R.string.auto_weekly_data_unavailable)
        }
    }
}
