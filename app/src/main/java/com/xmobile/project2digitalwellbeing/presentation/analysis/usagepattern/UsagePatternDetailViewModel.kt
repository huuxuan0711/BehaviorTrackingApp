package com.xmobile.project2digitalwellbeing.presentation.analysis.usagepattern

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase.GetUsagePatternExperienceOutcome
import com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase.GetUsagePatternExperienceUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetUsagePatternDataParams
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
    private val getUsagePatternExperienceUseCase: GetUsagePatternExperienceUseCase
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()
    private val _uiState = MutableStateFlow(UsagePatternDetailUiState())
    val uiState: StateFlow<UsagePatternDetailUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val outcome = getUsagePatternExperienceUseCase(
                GetUsagePatternDataParams(
                    nowMillis = System.currentTimeMillis(),
                    timezoneId = ZoneId.systemDefault().id
                )
            )

            when (outcome) {
                is GetUsagePatternExperienceOutcome.Success -> {
                    val data = outcome.data.data
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
                        insightText = outcome.data.insightSummaryText
                    )
                }

                is GetUsagePatternExperienceOutcome.Failure -> {
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
