package com.xmobile.project2digitalwellbeing.presentation.analysis.latenight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase.GetLateNightAnalysisExperienceOutcome
import com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase.GetLateNightAnalysisExperienceUseCase
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataOutcome
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataParams
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetLateNightAnalysisDataParams
import com.xmobile.project2digitalwellbeing.helper.UsageFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class LateNightAnalysisViewModel @Inject constructor(
    application: Application,
    private val refreshUsageDataUseCase: RefreshUsageDataUseCase,
    private val getLateNightAnalysisExperienceUseCase: GetLateNightAnalysisExperienceUseCase
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()

    private val _uiState = MutableStateFlow(LateNightAnalysisUiState())
    val uiState: StateFlow<LateNightAnalysisUiState> = _uiState.asStateFlow()
    private var hasLoadedData = false

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val nowMillis = System.currentTimeMillis()
            val timezoneId = ZoneId.systemDefault().id
            val refreshError = if (!hasLoadedData) {
                when (
                    val refreshOutcome = refreshUsageDataUseCase(
                        RefreshUsageDataParams(
                            nowMillis = nowMillis,
                            timezoneId = timezoneId,
                            forceFullRefresh = false
                        )
                    )
                ) {
                    is RefreshUsageDataOutcome.Success -> null
                    is RefreshUsageDataOutcome.Failure -> refreshOutcome.error.toUserMessage()
                }
            } else {
                null
            }

            val params = GetLateNightAnalysisDataParams(
                nowMillis = nowMillis,
                timezoneId = timezoneId
            )

            when (val outcome = getLateNightAnalysisExperienceUseCase(params)) {
                is GetLateNightAnalysisExperienceOutcome.Success -> {
                    hasLoadedData = true
                    val data = outcome.data.data
                    
                    // Map hourly usage to the fixed list of 8 bars (22h -> 5h)
                    val lateNightHours = listOf(22, 23, 0, 1, 2, 3, 4, 5)
                    val hourlyMap = data.hourlyUsage.associateBy { it.hourOfDay }
                    val chartValues = lateNightHours.map { hour ->
                        (hourlyMap[hour]?.totalTimeMillis ?: 0L) / (60f * 1000f)
                    }

                    val topApps = data.topApps.map { app ->
                        LateNightAppUiModel(
                            packageName = app.packageName,
                            appName = app.appName ?: app.packageName,
                            durationText = UsageFormatter.formatDuration(context, app.totalTimeMillis)
                        )
                    }

                    val peakLabel = if (data.peakUsageWindowStartHour != null) {
                        context.getString(R.string.auto_peak_usage_at, formatHour(data.peakUsageWindowStartHour!!))
                    } else {
                        context.getString(R.string.auto_no_peak_usage_detected)
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            totalScreenTime = UsageFormatter.formatDuration(context, data.totalScreenTimeMillis),
                            sessionCount = data.totalSessionCount.toString(),
                            avgDuration = formatDurationPrecise(data.averageSessionLengthMillis),
                            hourlyUsage = chartValues,
                            topApps = topApps,
                            peakUsageLabel = peakLabel,
                            insightText = refreshError ?: outcome.data.insightSummaryText,
                            recommendationText = data.recommendation,
                            errorMessage = refreshError
                        )
                    }
                }
                is GetLateNightAnalysisExperienceOutcome.Failure -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = refreshError ?: context.getString(R.string.auto_late_night_data_unavailable)
                        ) 
                    }
                }
            }
        }
    }

    private fun formatDurationPrecise(millis: Long): String {
        val totalSeconds = millis / 1000
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60
        return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
    }

    private fun formatHour(hour: Int): String {
        return when {
            hour == 0 -> "12 AM"
            hour == 12 -> "12 PM"
            hour > 12 -> "${hour - 12} PM"
            else -> "$hour AM"
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
}
