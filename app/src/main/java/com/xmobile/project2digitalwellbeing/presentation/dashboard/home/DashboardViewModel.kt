package com.xmobile.project2digitalwellbeing.presentation.dashboard.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataOutcome
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataParams
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetDashboardDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetDashboardDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetDashboardDataUseCase
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
    private val getDashboardDataUseCase: GetDashboardDataUseCase
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
            DashboardAction.OpenBehaviorInsight -> DashboardEffect.OpenBehaviorInsight
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

            val refreshErrorMessage = if (forceRefresh || !hasLoadedData) {
                when (val refreshOutcome = refreshUsageDataUseCase(
                    RefreshUsageDataParams(
                        nowMillis = nowMillis,
                        timezoneId = timezoneId,
                        forceFullRefresh = forceRefresh
                    )
                )) {
                    is RefreshUsageDataOutcome.Success -> null
                    is RefreshUsageDataOutcome.Failure -> refreshOutcome.error.toUserMessage()
                }
            } else {
                null
            }

            when (val outcome = getDashboardDataUseCase(
                GetDashboardDataParams(
                    nowMillis = nowMillis,
                    timezoneId = timezoneId
                )
            )) {
                is GetDashboardDataOutcome.Success -> {
                    hasLoadedData = true
                    _uiState.value = DashboardUiState(
                        isLoading = false,
                        currentDateLabel = outcome.data.currentLocalDate.toFriendlyDate(timezoneId),
                        dailyUsage = outcome.data.dailyUsage,
                        topInsight = outcome.data.topInsight,
                        hourlyUsage = outcome.data.hourlyUsage,
                        topApps = outcome.data.topApps,
                        errorMessage = refreshErrorMessage
                    )
                }

                is GetDashboardDataOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentDateLabel = nowMillis.toFriendlyDate(timezoneId),
                            errorMessage = refreshErrorMessage ?: outcome.error.toUserMessage()
                        )
                    }
                }
            }
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
