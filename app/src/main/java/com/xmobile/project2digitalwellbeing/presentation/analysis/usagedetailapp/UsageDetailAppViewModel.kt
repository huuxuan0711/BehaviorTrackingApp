package com.xmobile.project2digitalwellbeing.presentation.analysis.usagedetailapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetUsageDetailAppUiStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class UsageDetailAppViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val getUsageDetailAppUiStateUseCase: GetUsageDetailAppUiStateUseCase
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()
    private val packageName: String = savedStateHandle.get<String>("PACKAGE_NAME") ?: ""

    private val _uiState = MutableStateFlow(UsageDetailAppUiState(packageName = packageName))
    val uiState: StateFlow<UsageDetailAppUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        if (packageName.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = context.getString(R.string.auto_error_missing_app_package)
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                getUsageDetailAppUiStateUseCase(packageName)
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = context.getString(R.string.auto_error_usage_detail_unavailable)
                    )
                }
            }
        }
    }
}
