package com.xmobile.project2digitalwellbeing.presentation.dashboard.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataOutcome
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataParams
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.DashboardDataError
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetDashboardDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetDashboardDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetDashboardDataUseCase
import com.xmobile.project2digitalwellbeing.helper.UsageFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DailyOverviewViewModel @Inject constructor(
    private val refreshUsageDataUseCase: RefreshUsageDataUseCase,
    private val getDashboardDataUseCase: GetDashboardDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyOverviewUiState())
    val uiState: StateFlow<DailyOverviewUiState> = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<DailyOverviewEffect>()
    val effects: SharedFlow<DailyOverviewEffect> = _effects.asSharedFlow()

    private var hasLoadedData = false

    fun onPermissionMissing() {
        viewModelScope.launch {
            _effects.emit(DailyOverviewEffect.OpenPermission)
        }
    }

    fun onDateSelectorClicked() {
        viewModelScope.launch {
            _effects.emit(DailyOverviewEffect.ShowDatePicker(_uiState.value.selectedDate))
        }
    }

    fun onDateSelected(date: LocalDate) {
        load(forceRefresh = false, targetDate = date)
    }

    fun load(forceRefresh: Boolean, targetDate: LocalDate? = _uiState.value.selectedDate) {
        val timezoneId = ZoneId.systemDefault().id
        val zoneId = ZoneId.of(timezoneId)
        val today = LocalDate.now(zoneId)
        val resolvedDate = targetDate ?: today
        val nowMillis = if (resolvedDate == today) {
            System.currentTimeMillis()
        } else {
            resolvedDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        }
        val shouldRefreshCurrentDay = resolvedDate == today

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    selectedDate = resolvedDate,
                    errorMessage = null
                )
            }

            val refreshError = if (shouldRefreshCurrentDay && (forceRefresh || !hasLoadedData)) {
                when (
                    val refreshOutcome = refreshUsageDataUseCase(
                        RefreshUsageDataParams(
                            nowMillis = nowMillis,
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

            val todayOutcome = getDashboardDataUseCase(
                GetDashboardDataParams(
                    nowMillis = nowMillis,
                    timezoneId = timezoneId
                )
            )
            val yesterdayOutcome = getDashboardDataUseCase(
                GetDashboardDataParams(
                    nowMillis = nowMillis - MILLIS_PER_DAY,
                    timezoneId = timezoneId
                )
            )

            when (todayOutcome) {
                is GetDashboardDataOutcome.Success -> {
                    hasLoadedData = true
                    val todayUsage = todayOutcome.data.dailyUsage
                    val yesterdayUsage = (yesterdayOutcome as? GetDashboardDataOutcome.Success)
                        ?.data
                        ?.dailyUsage
                    val topInsight = todayOutcome.data.topInsight

                    _uiState.value = DailyOverviewUiState(
                        isLoading = false,
                        selectedDate = resolvedDate,
                        dateLabel = UsageFormatter.formatFriendlyDate(
                            LocalDate.parse(todayOutcome.data.currentLocalDate),
                            today
                        ),
                        totalScreenTimeText = UsageFormatter.formatDuration(todayUsage.totalScreenTimeMillis),
                        compareText = buildCompareText(
                            todayMillis = todayUsage.totalScreenTimeMillis,
                            yesterdayMillis = yesterdayUsage?.totalScreenTimeMillis
                        ),
                        sessionCountText = todayUsage.totalSessionCount.toString(),
                        longestSessionText = todayUsage.sessions
                            .maxOfOrNull { it.durationMillis }
                            ?.let { UsageFormatter.formatDuration(it) }
                            ?: "0m",
                        hourlyUsage = todayOutcome.data.hourlyUsage,
                        topApps = todayOutcome.data.topApps,
                        insightText = topInsight?.let { "${it.description} (${it.score}/100)" }
                            ?: "No insights are available yet for today.",
                        errorMessage = refreshError
                    )
                }

                is GetDashboardDataOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedDate = resolvedDate,
                            dateLabel = UsageFormatter.formatFriendlyDate(nowMillis, timezoneId, today),
                            errorMessage = refreshError ?: todayOutcome.error.toUserMessage()
                        )
                    }
                }
            }
        }
    }

    private fun buildCompareText(todayMillis: Long, yesterdayMillis: Long?): String {
        if (yesterdayMillis == null || yesterdayMillis <= 0L) {
            return "No yesterday data yet"
        }
        val deltaPercent = (((todayMillis - yesterdayMillis).toDouble() / yesterdayMillis.toDouble()) * 100)
            .toInt()
        return when {
            deltaPercent > 0 -> "${deltaPercent}% more than yesterday"
            deltaPercent < 0 -> "${kotlin.math.abs(deltaPercent)}% less than yesterday"
            else -> "Same as yesterday"
        }
    }

    private fun com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageDataError.toUserMessage(): String {
        return when (this) {
            is com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageDataError.PermissionDenied ->
                "Usage access is required to refresh your daily overview."

            is com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageDataError.InvalidTimeZone ->
                "Your device time zone could not be resolved."

            else -> "The latest usage data could not be refreshed."
        }
    }

    private fun DashboardDataError.toUserMessage(): String {
        return when (this) {
            is DashboardDataError.InvalidTimeZone ->
                "Your device time zone could not be resolved."

            else -> "Daily overview data is not available yet."
        }
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }
}
