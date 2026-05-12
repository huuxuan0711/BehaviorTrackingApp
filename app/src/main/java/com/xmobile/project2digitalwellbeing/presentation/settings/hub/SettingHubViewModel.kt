package com.xmobile.project2digitalwellbeing.presentation.settings.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.domain.preferences.usecase.ObserveUsageAnalysisPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingHubViewModel @Inject constructor(
    observePreferencesUseCase: ObserveUsageAnalysisPreferencesUseCase
) : ViewModel() {

    val preferences = observePreferencesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
