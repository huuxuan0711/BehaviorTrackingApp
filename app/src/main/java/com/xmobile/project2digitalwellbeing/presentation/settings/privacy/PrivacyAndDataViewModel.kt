package com.xmobile.project2digitalwellbeing.presentation.settings.privacy

import android.app.Application
import android.app.backup.BackupManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.domain.preferences.usecase.ObserveUsageAnalysisPreferencesUseCase
import com.xmobile.project2digitalwellbeing.domain.preferences.usecase.UpdateUsageAnalysisPreferencesUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.DeleteAllUsageDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.ExportUsageDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.ResetBehaviorAnalysisUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacyAndDataViewModel @Inject constructor(
    application: Application,
    observePreferencesUseCase: ObserveUsageAnalysisPreferencesUseCase,
    private val updatePreferencesUseCase: UpdateUsageAnalysisPreferencesUseCase,
    private val deleteAllUsageDataUseCase: DeleteAllUsageDataUseCase,
    private val resetBehaviorAnalysisUseCase: ResetBehaviorAnalysisUseCase,
    private val exportUsageDataUseCase: ExportUsageDataUseCase
) : AndroidViewModel(application) {

    private val backupManager = BackupManager(application)

    val preferences = observePreferencesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun toggleCloudBackup(enabled: Boolean) {
        viewModelScope.launch {
            updatePreferencesUseCase { it.copy(isCloudBackupEnabled = enabled) }
            backupManager.dataChanged()
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            deleteAllUsageDataUseCase()
        }
    }

    fun resetAnalysis() {
        viewModelScope.launch {
            resetBehaviorAnalysisUseCase()
        }
    }

    fun exportData(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val data = exportUsageDataUseCase()
            onResult(data)
        }
    }
}
