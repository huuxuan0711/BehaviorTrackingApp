package com.xmobile.project2digitalwellbeing.presentation.analysis.appcategory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.apps.usecase.GetAppCategoryDataUseCase
import com.xmobile.project2digitalwellbeing.domain.apps.usecase.UpdateAppCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppFocusGroup
import com.xmobile.project2digitalwellbeing.helper.UsageFormatter

@HiltViewModel
class AppCategoryViewModel @Inject constructor(
    private val getAppCategoryDataUseCase: GetAppCategoryDataUseCase,
    private val updateAppCategoryUseCase: UpdateAppCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppCategoryUiState())
    val uiState: StateFlow<AppCategoryUiState> = _uiState.asStateFlow()

    private var allCategories: List<CategoryGroupUiModel> = emptyList()

    private var isInitialLoad = true

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            if (isInitialLoad) {
                _uiState.update { it.copy(isLoading = true) }
            }
            try {
                val data = getAppCategoryDataUseCase()
                allCategories = data.map { (category, apps) ->
                    CategoryGroupUiModel(
                        category = category,
                        isExpanded = true,
                        apps = apps.map {
                            val durationText = "${UsageFormatter.formatDuration(it.totalTimeMillis)} past 24h"
                            AppItemUiModel(
                                packageName = it.packageName,
                                appName = it.appName ?: it.packageName,
                                category = it.category,
                                focusGroup = category,
                                usageText = durationText
                            )
                        }
                    )
                }
                isInitialLoad = false
                filterAndApply(uiState.value.searchQuery)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        filterAndApply(query)
    }

    fun toggleCategoryExpansion(category: com.xmobile.project2digitalwellbeing.domain.apps.model.AppFocusGroup) {
        _uiState.update { state ->
            state.copy(
                categories = state.categories.map {
                    if (it.category == category) it.copy(isExpanded = !it.isExpanded) else it
                }
            )
        }
    }

    fun updateAppCategory(packageName: String, newCategory: AppCategory) {
        viewModelScope.launch {
            try {
                updateAppCategoryUseCase(packageName, newCategory)
                load()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update category") }
            }
        }
    }

    private fun filterAndApply(query: String) {
        val filtered = if (query.isBlank()) {
            allCategories
        } else {
            allCategories.mapNotNull { group ->
                val filteredApps = group.apps.filter {
                    it.appName.contains(query, ignoreCase = true)
                }
                if (filteredApps.isNotEmpty()) {
                    group.copy(apps = filteredApps, isExpanded = true)
                } else null
            }
        }

        // Preserve expansion state from current uiState
        val currentExpansionStates = _uiState.value.categories.associate { it.category to it.isExpanded }
        val finalCategories = filtered.map { group ->
            group.copy(isExpanded = currentExpansionStates[group.category] ?: group.isExpanded)
        }

        _uiState.update { it.copy(isLoading = false, categories = finalCategories) }
    }
}
