package com.xmobile.project2digitalwellbeing.presentation.settings.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.domain.insights.model.InsightSensitivity
import com.xmobile.project2digitalwellbeing.domain.preferences.usecase.ObserveUsageAnalysisPreferencesUseCase
import com.xmobile.project2digitalwellbeing.domain.preferences.usecase.UpdateUsageAnalysisPreferencesUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val observeUsageAnalysisPreferencesUseCase: ObserveUsageAnalysisPreferencesUseCase,
    private val updateUsageAnalysisPreferencesUseCase: UpdateUsageAnalysisPreferencesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreferencesUiState())
    val uiState: StateFlow<PreferencesUiState> = _uiState.asStateFlow()
    private val actions = MutableSharedFlow<PreferencesAction>(extraBufferCapacity = 64)

    init {
        observeUsageAnalysisPreferencesUseCase()
            .onEach { prefs ->
                _uiState.update { it.fromDomain(prefs) }
            }
            .launchIn(viewModelScope)

        actions
            .onEach(::handleAction)
            .launchIn(viewModelScope)
    }

    fun onLateNightThresholdChanged(progress: Int) {
        actions.tryEmit(PreferencesAction.LateNightThresholdChanged(progress))
    }

    fun onSessionLengthThresholdChanged(progress: Int) {
        actions.tryEmit(PreferencesAction.SessionLengthThresholdChanged(progress))
    }

    fun onTrackAllCategoriesChanged(enabled: Boolean) {
        actions.tryEmit(PreferencesAction.TrackAllCategoriesChanged(enabled))
    }

    fun onInsightNotificationsChanged(enabled: Boolean) {
        actions.tryEmit(PreferencesAction.InsightNotificationsChanged(enabled))
    }

    fun onDarkModeChanged(enabled: Boolean) {
        actions.tryEmit(PreferencesAction.DarkModeChanged(enabled))
    }

    fun onWeeklyReportsChanged(enabled: Boolean) {
        actions.tryEmit(PreferencesAction.WeeklyReportsChanged(enabled))
    }

    fun onCloudEnhancementChanged(enabled: Boolean) {
        actions.tryEmit(PreferencesAction.CloudEnhancementChanged(enabled))
    }

    fun onInsightSensitivityChanged(progress: Int) {
        actions.tryEmit(PreferencesAction.InsightSensitivityChanged(progress))
    }

    fun onLanguageChanged(languageCode: String) {
        actions.tryEmit(PreferencesAction.LanguageChanged(languageCode))
    }

    private suspend fun handleAction(action: PreferencesAction) {
        updateUsageAnalysisPreferencesUseCase { current ->
            when (action) {
                is PreferencesAction.LateNightThresholdChanged -> {
                    val hour = progressToLateNightHour(action.progress)
                    current.copy(lateNightStartHour = hour)
                }

                is PreferencesAction.SessionLengthThresholdChanged -> {
                    val minutes = progressToSessionLengthMinutes(action.progress)
                    current.copy(longSessionThresholdMillis = minutes * 60L * 1000L)
                }

                is PreferencesAction.TrackAllCategoriesChanged ->
                    current.copy(trackAllCategories = action.enabled)

                is PreferencesAction.InsightNotificationsChanged ->
                    current.copy(insightNotificationsEnabled = action.enabled)

                is PreferencesAction.DarkModeChanged ->
                    current.copy(darkModeEnabled = action.enabled)

                is PreferencesAction.WeeklyReportsChanged ->
                    current.copy(weeklyReportsEnabled = action.enabled)

                is PreferencesAction.CloudEnhancementChanged ->
                    current.copy(cloudEnhancementEnabled = action.enabled)

                is PreferencesAction.InsightSensitivityChanged -> {
                    val sensitivity = when (action.progress) {
                        0 -> InsightSensitivity.LOW
                        2 -> InsightSensitivity.HIGH
                        else -> InsightSensitivity.MEDIUM
                    }
                    current.copy(insightSensitivity = sensitivity)
                }

                is PreferencesAction.LanguageChanged ->
                    current.copy(languageCode = action.languageCode)
            }
        }
    }

    private fun progressToLateNightHour(progress: Int): Int {
        return (20 + progress) % 24
    }

    private fun progressToSessionLengthMinutes(progress: Int): Int {
        return 10 + (progress * 5)
    }
}

private sealed interface PreferencesAction {
    data class LateNightThresholdChanged(val progress: Int) : PreferencesAction
    data class SessionLengthThresholdChanged(val progress: Int) : PreferencesAction
    data class TrackAllCategoriesChanged(val enabled: Boolean) : PreferencesAction
    data class InsightNotificationsChanged(val enabled: Boolean) : PreferencesAction
    data class DarkModeChanged(val enabled: Boolean) : PreferencesAction
    data class WeeklyReportsChanged(val enabled: Boolean) : PreferencesAction
    data class CloudEnhancementChanged(val enabled: Boolean) : PreferencesAction
    data class InsightSensitivityChanged(val progress: Int) : PreferencesAction
    data class LanguageChanged(val languageCode: String) : PreferencesAction
}

data class PreferencesUiState(
    val lateNightHour: Int = 22,
    val lateNightProgress: Int = 2,
    val sessionMinutes: Int = 20,
    val sessionProgress: Int = 2,
    val trackAllCategories: Boolean = true,
    val insightNotificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val weeklyReportsEnabled: Boolean = true,
    val cloudEnhancementEnabled: Boolean = false,
    val sensitivity: InsightSensitivity = InsightSensitivity.MEDIUM,
    val sensitivityProgress: Int = 1,
    val languageCode: String = "en"
) {
    fun fromDomain(prefs: UsageAnalysisPreferences): PreferencesUiState {
        val lateNightProgress = hourToLateNightProgress(prefs.lateNightStartHour)
        val sessionMinutes = (prefs.longSessionThresholdMillis / (60L * 1000L)).toInt()
        val sessionProgress = (sessionMinutes - 10) / 5
        val sensitivityProgress = when (prefs.insightSensitivity) {
            InsightSensitivity.LOW -> 0
            InsightSensitivity.MEDIUM -> 1
            InsightSensitivity.HIGH -> 2
        }

        return copy(
            lateNightHour = prefs.lateNightStartHour,
            lateNightProgress = lateNightProgress,
            sessionMinutes = sessionMinutes,
            sessionProgress = sessionProgress,
            trackAllCategories = prefs.trackAllCategories,
            insightNotificationsEnabled = prefs.insightNotificationsEnabled,
            darkModeEnabled = prefs.darkModeEnabled,
            weeklyReportsEnabled = prefs.weeklyReportsEnabled,
            cloudEnhancementEnabled = prefs.cloudEnhancementEnabled,
            sensitivity = prefs.insightSensitivity,
            sensitivityProgress = sensitivityProgress,
            languageCode = prefs.languageCode
        )
    }

    private fun hourToLateNightProgress(hour: Int): Int {
        return if (hour >= 20) hour - 20 else hour + 4
    }
}
