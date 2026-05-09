package com.xmobile.project2digitalwellbeing.presentation.analysis.usagepattern

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.R
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
    application: Application,
    private val getUsagePatternDataUseCase: GetUsagePatternDataUseCase
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()
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
                            sessionCountText = context.getString(R.string.auto_text_n_sessions, it.launchCount),
                            progressRatio = if (maxLaunchCount <= 0) 0f else {
                                it.launchCount.toFloat() / maxLaunchCount.toFloat()
                            }
                        )
                    }

                    _uiState.value = UsagePatternDetailUiState(
                        isLoading = false,
                        averageSessionText = UsageFormatter.formatDurationVerbose(context, data.averageSessionLengthMillis),
                        longestSessionText = UsageFormatter.formatDurationVerbose(context, data.longestSessionMillis),
                        totalSessionText = data.totalSessionCount.toString(),
                        switchCountText = data.switchCount.toString(),
                        averageSwitchIntervalText = UsageFormatter.formatDurationVerbose(context, data.averageSwitchIntervalMillis),
                        shortSessionText = context.getString(R.string.auto_text_n_sessions, distribution.shortSessionCount),
                        mediumSessionText = context.getString(R.string.auto_text_n_sessions, distribution.mediumSessionCount),
                        longSessionText = context.getString(R.string.auto_text_n_sessions, distribution.longSessionCount),
                        shortSessionRatio = distribution.shortSessionCount.toFloat() / totalDistributionCount.toFloat(),
                        mediumSessionRatio = distribution.mediumSessionCount.toFloat() / totalDistributionCount.toFloat(),
                        longSessionRatio = distribution.longSessionCount.toFloat() / totalDistributionCount.toFloat(),
                        topApps = topApps,
                        insightText = data.topInsight?.let { insight ->
                            when (insight.type) {
                                InsightType.LATE_NIGHT_USAGE -> context.getString(R.string.auto_insight_late_night_usage_desc)
                                InsightType.FREQUENT_SWITCHING -> context.getString(R.string.auto_insight_frequent_switching_desc)
                                InsightType.BINGE_USAGE -> context.getString(R.string.auto_insight_binge_usage_desc)
                                InsightType.LATE_NIGHT_SWITCHING -> context.getString(R.string.auto_insight_late_night_switching_desc)
                                InsightType.WORK_HOUR_DISTRACTION -> context.getString(R.string.auto_insight_work_hour_distraction_desc)
                                InsightType.MORNING_ROUTINE -> context.getString(R.string.auto_insight_morning_routine_desc)
                                InsightType.CONSTANT_CHECKING -> context.getString(R.string.auto_insight_constant_checking_desc)
                                InsightType.APP_RELIANCE -> context.getString(R.string.auto_insight_app_reliance_desc)
                            }
                        } ?: UsagePatternDetailUiState.DEFAULT_INSIGHT
                    )
                }

                is GetUsagePatternDataOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = outcome.error.toUserMessage(context)
                        )
                    }
                }
            }
        }
    }

    private fun UsagePatternDataError.toUserMessage(context: android.content.Context): String {
        return when (this) {
            is UsagePatternDataError.InvalidTimeZone -> context.getString(R.string.auto_error_invalid_timezone)
            is UsagePatternDataError.DataAccessFailure -> context.getString(R.string.auto_error_data_access_failure)
            is UsagePatternDataError.ProcessingFailure -> context.getString(R.string.auto_error_processing_failure)
        }
    }
}
