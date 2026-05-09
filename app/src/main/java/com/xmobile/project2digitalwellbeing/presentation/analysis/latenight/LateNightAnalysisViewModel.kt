package com.xmobile.project2digitalwellbeing.presentation.analysis.latenight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetLateNightAnalysisDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetLateNightAnalysisDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetLateNightAnalysisDataUseCase
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
    private val getLateNightAnalysisDataUseCase: GetLateNightAnalysisDataUseCase
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()

    private val _uiState = MutableStateFlow(LateNightAnalysisUiState())
    val uiState: StateFlow<LateNightAnalysisUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val params = GetLateNightAnalysisDataParams(
                nowMillis = System.currentTimeMillis(),
                timezoneId = ZoneId.systemDefault().id
            )

            when (val outcome = getLateNightAnalysisDataUseCase(params)) {
                is GetLateNightAnalysisDataOutcome.Success -> {
                    val data = outcome.data
                    
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
                        "Peak usage at ${formatHour(data.peakUsageWindowStartHour!!)}"
                    } else {
                        "No peak usage detected"
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
                            insightText = data.insightSummary,
                            recommendationText = data.recommendation
                        )
                    }
                }
                is GetLateNightAnalysisDataOutcome.Failure -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = "Failed to load analysis data"
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
}
