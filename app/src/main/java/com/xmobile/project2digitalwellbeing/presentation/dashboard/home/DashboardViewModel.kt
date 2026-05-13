package com.xmobile.project2digitalwellbeing.presentation.dashboard.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase.GetDashboardExperienceOutcome
import com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase.GetDashboardExperienceUseCase
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataOutcome
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataParams
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetDashboardDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.DashboardQueryMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val refreshUsageDataUseCase: RefreshUsageDataUseCase,
    private val getDashboardExperienceUseCase: GetDashboardExperienceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<DashboardEffect>()
    val effects: SharedFlow<DashboardEffect> = _effects.asSharedFlow()

    private var hasLoadedData = false

    fun onAction(action: DashboardAction, hasPermission: Boolean) {
        val grantedEffect = when (action) {
            DashboardAction.OpenDailyOverview -> DashboardEffect.OpenDailyOverview
            DashboardAction.OpenWeeklyOverview -> DashboardEffect.OpenWeeklyOverview
            DashboardAction.OpenSessionTimeline -> DashboardEffect.OpenSessionTimeline
        }
        emitNavigationEffect(hasPermission, grantedEffect)
    }

    fun onPermissionMissing() {
        viewModelScope.launch {
            _effects.emit(DashboardEffect.RequestPermission)
        }
    }

    fun load(forceRefresh: Boolean) {
        val nowMillis = System.currentTimeMillis()
        val timezoneId = ZoneId.systemDefault().id

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            // Step 1: refresh local cache from UsageStats (write path).
            val refreshWarningMessage = refreshLocalUsageCacheIfNeeded(
                nowMillis = nowMillis,
                timezoneId = timezoneId,
                forceRefresh = forceRefresh
            )

            // Step 2: read composed dashboard experience from local data (read path).
            when (val outcome = getDashboardExperienceUseCase(
                GetDashboardDataParams(
                    nowMillis = nowMillis,
                    timezoneId = timezoneId,
                    queryMode = DashboardQueryMode.Sliding24Hours
                )
            )) {
                is GetDashboardExperienceOutcome.Success -> {
                    hasLoadedData = true
                    val dashboardData = outcome.data.dashboardData
                    val state = DashboardUiState(
                        isLoading = false,
                        currentDateLabel = dashboardData.currentLocalDate.toFriendlyDate(timezoneId),
                        dailyUsage = dashboardData.dailyUsage,
                        hourlyUsage = dashboardData.hourlyUsage,
                        topApps = dashboardData.topApps,
                        errorMessage = refreshWarningMessage,
                        insightSummaryText = outcome.data.insightSummaryText
                    )
                    _uiState.value = state.copy(
                        lateNightRatioText = state.hourlyUsage.toLateNightRatioText()
                    )
                }

                is GetDashboardExperienceOutcome.Failure -> {
                    _uiState.update {
                        val error = refreshWarningMessage ?: outcome.error.toUserMessage()
                        it.copy(
                            isLoading = false,
                            currentDateLabel = nowMillis.toFriendlyDate(timezoneId),
                            errorMessage = error,
                            insightSummaryText = error
                        )
                    }
                }
            }
        }
    }

    private suspend fun refreshLocalUsageCacheIfNeeded(
        nowMillis: Long,
        timezoneId: String,
        forceRefresh: Boolean
    ): String? {
        if (!forceRefresh && hasLoadedData) return null
        return when (val refreshOutcome = refreshUsageDataUseCase(
            RefreshUsageDataParams(
                nowMillis = nowMillis,
                timezoneId = timezoneId,
                forceFullRefresh = forceRefresh
            )
        )) {
            is RefreshUsageDataOutcome.Success -> null
            is RefreshUsageDataOutcome.Failure -> refreshOutcome.error.toUserMessage()
        }
    }

    private fun emitNavigationEffect(
        hasPermission: Boolean,
        grantedEffect: DashboardEffect
    ) {
        viewModelScope.launch {
            _effects.emit(
                if (hasPermission) grantedEffect else DashboardEffect.RequestPermission
            )
        }
    }

    private fun Long.toFriendlyDate(timezoneId: String): String {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.of(timezoneId))
            .format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                    .withLocale(Locale.getDefault())
            )
    }

    private fun String.toFriendlyDate(timezoneId: String): String {
        return java.time.LocalDate.parse(this)
            .atStartOfDay(ZoneId.of(timezoneId))
            .format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                    .withLocale(Locale.getDefault())
            )
    }

    private fun com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageDataError.toUserMessage(): String {
        return when (this) {
            is com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageDataError.PermissionDenied ->
                "Usage access is required to refresh your dashboard."

            is com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageDataError.InvalidTimeZone ->
                "Your device time zone could not be resolved."

            else -> "The latest usage data could not be refreshed."
        }
    }

    private fun com.xmobile.project2digitalwellbeing.domain.usage.usecase.DashboardDataError.toUserMessage(): String {
        return when (this) {
            is com.xmobile.project2digitalwellbeing.domain.usage.usecase.DashboardDataError.InvalidTimeZone ->
                "Your device time zone could not be resolved."

            else -> "Dashboard data is not available yet."
        }
    }
}
