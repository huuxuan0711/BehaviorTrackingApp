package com.xmobile.project2digitalwellbeing.presentation.analysis.appcategory

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppFocusGroup

data class AppCategoryUiState(
    val isLoading: Boolean = false,
    val categories: List<CategoryGroupUiModel> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null
)

data class CategoryGroupUiModel(
    val category: AppFocusGroup,
    val apps: List<AppItemUiModel>,
    val isExpanded: Boolean = false
)

data class AppItemUiModel(
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val usageText: String
)
