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
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
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
                        dateLabel = todayOutcome.data.currentLocalDate.toFriendlyDate(timezoneId, today),
                        totalScreenTimeText = todayUsage.totalScreenTimeMillis.toDurationText(),
                        compareText = buildCompareText(
                            todayMillis = todayUsage.totalScreenTimeMillis,
                            yesterdayMillis = yesterdayUsage?.totalScreenTimeMillis
                        ),
                        sessionCountText = todayUsage.totalSessionCount.toString(),
                        longestSessionText = todayUsage.sessions
                            .maxOfOrNull { it.durationMillis }
                            ?.toDurationText()
                            ?: "0m",
                        hourlyUsage = todayOutcome.data.hourlyUsage,
                        topApps = todayOutcome.data.topApps,
                        insightText = topInsight?.let { "${it.type.toFriendlyLabel()} (${it.score}/100)" }
                            ?: "No insights are available yet for today.",
                        errorMessage = refreshError
                    )
                }

                is GetDashboardDataOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedDate = resolvedDate,
                            dateLabel = nowMillis.toFriendlyDate(timezoneId, today),
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

    private fun Long.toFriendlyDate(timezoneId: String, today: LocalDate): String {
        val localDate = Instant.ofEpochMilli(this)
            .atZone(ZoneId.of(timezoneId))
            .toLocalDate()
        return localDate.toFriendlyDate(today)
    }

    private fun String.toFriendlyDate(timezoneId: String, today: LocalDate): String {
        return java.time.LocalDate.parse(this).toFriendlyDate(today)
    }

    private fun LocalDate.toFriendlyDate(today: LocalDate): String {
        return if (this == today) {
            "Today"
        } else {
            format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                    .withLocale(Locale.getDefault())
            )
        }
    }

    private fun Long.toDurationText(): String {
        val totalMinutes = this / (60L * 1000L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
            hours > 0L -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    private fun com.xmobile.project2digitalwellbeing.domain.insights.model.InsightType.toFriendlyLabel(): String {
        return name.lowercase()
            .split('_')
            .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
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
