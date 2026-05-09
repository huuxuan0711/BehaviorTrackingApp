package com.xmobile.project2digitalwellbeing.presentation.dashboard.daily

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.R
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
import java.time.LocalTime
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
    application: Application,
    private val refreshUsageDataUseCase: RefreshUsageDataUseCase,
    private val getDashboardDataUseCase: GetDashboardDataUseCase
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()
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
            // For historical days, use the end of that day to ensure we get the full day's usage
            // instead of just up to 00:00:00 (which would be empty)
            resolvedDate.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
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
                    is RefreshUsageDataOutcome.Failure -> refreshOutcome.error.toUserMessage(context)
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
                            context,
                            LocalDate.parse(todayOutcome.data.currentLocalDate),
                            today
                        ),
                        totalScreenTimeText = UsageFormatter.formatDuration(context, todayUsage.totalScreenTimeMillis),
                        compareText = buildCompareText(
                            todayMillis = todayUsage.totalScreenTimeMillis,
                            yesterdayMillis = yesterdayUsage?.totalScreenTimeMillis
                        ),
                        sessionCountText = todayUsage.totalSessionCount.toString(),
                        longestSessionText = todayUsage.sessions
                            .maxOfOrNull { it.durationMillis }
                            ?.let { UsageFormatter.formatDuration(context, it) }
                            ?: context.getString(R.string.auto_text_0m),
                        hourlyUsage = todayOutcome.data.hourlyUsage,
                        topApps = todayOutcome.data.topApps,
                        insightText = topInsight?.description?.takeIf { it.isNotBlank() }
                            ?: context.getString(R.string.auto_no_daily_insight),
                        errorMessage = refreshError
                    )
                }

                is GetDashboardDataOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedDate = resolvedDate,
                            dateLabel = UsageFormatter.formatFriendlyDate(context, nowMillis, timezoneId, today),
                            errorMessage = refreshError ?: todayOutcome.error.toUserMessage(context)
                        )
                    }
                }
            }
        }
    }

    private fun buildCompareText(todayMillis: Long, yesterdayMillis: Long?): String {
        if (yesterdayMillis == null || yesterdayMillis <= 0L) {
            return context.getString(R.string.auto_no_yesterday_data)
        }
        val deltaPercent = (((todayMillis - yesterdayMillis).toDouble() / yesterdayMillis.toDouble()) * 100)
            .toInt()
        return when {
            deltaPercent > 0 -> context.getString(R.string.auto_compare_more, deltaPercent)
            deltaPercent < 0 -> context.getString(R.string.auto_compare_less, kotlin.math.abs(deltaPercent))
            else -> context.getString(R.string.auto_compare_same)
        }
    }

    private fun com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageDataError.toUserMessage(context: android.content.Context): String {
        return when (this) {
            is com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageDataError.PermissionDenied ->
                context.getString(R.string.auto_error_permission_denied)

            is com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageDataError.InvalidTimeZone ->
                context.getString(R.string.auto_error_invalid_timezone)

            else -> context.getString(R.string.auto_error_refresh_failure)
        }
    }

    private fun DashboardDataError.toUserMessage(context: android.content.Context): String {
        return when (this) {
            is DashboardDataError.InvalidTimeZone ->
                context.getString(R.string.auto_error_invalid_timezone)

            else -> context.getString(R.string.auto_error_no_dashboard_data)
        }
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }
}
