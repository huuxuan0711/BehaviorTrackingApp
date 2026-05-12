package com.xmobile.project2digitalwellbeing.presentation.dashboard.daily

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase.GetDashboardExperienceOutcome
import com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase.GetDashboardExperienceUseCase
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
    private val getDashboardDataUseCase: GetDashboardDataUseCase,
    private val getDashboardExperienceUseCase: GetDashboardExperienceUseCase
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
            val localizedContext = context.localizedForAppLocale()
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
                    is RefreshUsageDataOutcome.Failure -> refreshOutcome.error.toUserMessage(localizedContext)
                }
            } else {
                null
            }

            val todayOutcome = getDashboardExperienceUseCase(
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
                is GetDashboardExperienceOutcome.Success -> {
                    hasLoadedData = true
                    val todayUsage = todayOutcome.data.dashboardData.dailyUsage
                    val yesterdayUsage = (yesterdayOutcome as? GetDashboardDataOutcome.Success)
                        ?.data
                        ?.dailyUsage

                    _uiState.value = DailyOverviewUiState(
                        isLoading = false,
                        selectedDate = resolvedDate,
                        dateLabel = UsageFormatter.formatFriendlyDate(
                            localizedContext,
                            LocalDate.parse(todayOutcome.data.dashboardData.currentLocalDate),
                            today
                        ),
                        totalScreenTimeText = UsageFormatter.formatDuration(localizedContext, todayUsage.totalScreenTimeMillis),
                        compareText = buildCompareText(
                            localizedContext,
                            todayMillis = todayUsage.totalScreenTimeMillis,
                            yesterdayMillis = yesterdayUsage?.totalScreenTimeMillis
                        ),
                        sessionCountText = todayUsage.totalSessionCount.toString(),
                        longestSessionText = todayUsage.sessions
                            .maxOfOrNull { it.durationMillis }
                            ?.let { UsageFormatter.formatDuration(localizedContext, it) }
                            ?: localizedContext.getString(R.string.auto_text_0m),
                        hourlyUsage = todayOutcome.data.dashboardData.hourlyUsage,
                        topApps = todayOutcome.data.dashboardData.topApps,
                        insightText = todayOutcome.data.insightSummaryText,
                        errorMessage = refreshError
                    )
                }

                is GetDashboardExperienceOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedDate = resolvedDate,
                            dateLabel = UsageFormatter.formatFriendlyDate(localizedContext, nowMillis, timezoneId, today),
                            errorMessage = refreshError ?: todayOutcome.error.toUserMessage(localizedContext)
                        )
                    }
                }
            }
        }
    }

    private fun buildCompareText(context: Context, todayMillis: Long, yesterdayMillis: Long?): String {
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

    private fun Context.localizedForAppLocale(): Context {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val firstLocale = appLocales[0] ?: return this
        val configuration = Configuration(resources.configuration)
        Locale.setDefault(firstLocale)
        configuration.setLocales(LocaleList(firstLocale))
        return createConfigurationContext(configuration)
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
