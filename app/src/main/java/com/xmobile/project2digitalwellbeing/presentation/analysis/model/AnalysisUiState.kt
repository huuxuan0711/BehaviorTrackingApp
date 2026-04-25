package com.xmobile.project2digitalwellbeing.presentation.analysis.model

import com.xmobile.project2digitalwellbeing.domain.usage.model.Insight

data class AnalysisUiState(
    val isLoading: Boolean = false,
    val insights: List<Insight> = emptyList(),
    val errorMessage: String? = null
)
