package com.xmobile.project2digitalwellbeing.presentation.analysis.usagepattern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.domain.insights.model.InsightType
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetUsagePatternDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetUsagePatternDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetUsagePatternDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.UsagePatternDataError
import com.xmobile.project2digitalwellbeing.helper.UsageFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class UsagePatternDetailViewModel @Inject constructor(
    private val getUsagePatternDataUseCase: GetUsagePatternDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsagePatternDetailUiState())
    val uiState: StateFlow<UsagePatternDetailUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val outcome = getUsagePatternDataUseCase(
                GetUsagePatternDataParams(
                    nowMillis = System.currentTimeMillis(),
                    timezoneId = ZoneId.systemDefault().id
                )
            )

            when (outcome) {
                is GetUsagePatternDataOutcome.Success -> {
                    val data = outcome.data
                    val distribution = data.sessionLengthDistribution
                    val totalDistributionCount = (
                        distribution.shortSessionCount +
                            distribution.mediumSessionCount +
                            distribution.longSessionCount
                        ).coerceAtLeast(1)
                    val maxLaunchCount = data.topAppsByLaunchCount.maxOfOrNull { it.launchCount } ?: 1

                    val topApps = data.topAppsByLaunchCount.map {
                        UsagePatternTopAppUiModel(
                            packageName = it.packageName,
                            appName = it.appName ?: it.packageName,
                            sessionCountText = "${it.launchCount} sessions",
                            progressRatio = if (maxLaunchCount <= 0) 0f else {
                                it.launchCount.toFloat() / maxLaunchCount.toFloat()
                            }
                        )
                    }

                    _uiState.value = UsagePatternDetailUiState(
                        isLoading = false,
                        averageSessionText = UsageFormatter.formatDurationVerbose(data.averageSessionLengthMillis),
                        longestSessionText = UsageFormatter.formatDurationVerbose(data.longestSessionMillis),
                        totalSessionText = data.totalSessionCount.toString(),
                        switchCountText = data.switchCount.toString(),
                        averageSwitchIntervalText = UsageFormatter.formatDurationVerbose(data.averageSwitchIntervalMillis),
                        shortSessionText = "${distribution.shortSessionCount} sessions",
                        mediumSessionText = "${distribution.mediumSessionCount} sessions",
                        longSessionText = "${distribution.longSessionCount} sessions",
                        shortSessionRatio = distribution.shortSessionCount.toFloat() / totalDistributionCount.toFloat(),
                        mediumSessionRatio = distribution.mediumSessionCount.toFloat() / totalDistributionCount.toFloat(),
                        longSessionRatio = distribution.longSessionCount.toFloat() / totalDistributionCount.toFloat(),
                        topApps = topApps,
                        insightText = data.topInsight?.let { insight ->
                            when (insight.type) {
                                InsightType.LATE_NIGHT_USAGE -> "A meaningful share of your usage happens late at night."
                                InsightType.FREQUENT_SWITCHING -> "You switch between apps frequently, which may fragment attention."
                                InsightType.BINGE_USAGE -> "Some sessions are long and uninterrupted in the same app."
                                InsightType.LATE_NIGHT_SWITCHING -> "Late-night usage also includes rapid app switching."
                                InsightType.WORK_HOUR_DISTRACTION -> "Distracting app usage appears during work hours."
                                InsightType.MORNING_ROUTINE -> "Your morning starts with relatively heavy phone usage."
                                InsightType.CONSTANT_CHECKING -> "You often check your phone in short bursts."
                                InsightType.APP_RELIANCE -> "A small set of apps dominates your launch behavior."
                            }
                        } ?: UsagePatternDetailUiState.DEFAULT_INSIGHT
                    )
                }

                is GetUsagePatternDataOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = outcome.error.toUserMessage()
                        )
                    }
                }
            }
        }
    }

    private fun UsagePatternDataError.toUserMessage(): String {
        return when (this) {
            is UsagePatternDataError.InvalidTimeZone -> "Your device time zone could not be resolved."
            is UsagePatternDataError.DataAccessFailure -> "Usage pattern data is not available yet."
            is UsagePatternDataError.ProcessingFailure -> "Usage pattern data could not be processed."
        }
    }
}
