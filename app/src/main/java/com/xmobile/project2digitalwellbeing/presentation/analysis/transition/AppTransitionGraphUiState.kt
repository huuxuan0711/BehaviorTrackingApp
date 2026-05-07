package com.xmobile.project2digitalwellbeing.presentation.analysis.transition

import com.xmobile.project2digitalwellbeing.domain.tracking.model.TransitionFilter
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.AnalysisTimeRange

data class AppTransitionGraphUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val timeRange: AnalysisTimeRange = AnalysisTimeRange.TODAY,
    val filter: TransitionFilter = TransitionFilter.ALL,
    val insightText: String = "No transition insight yet.",
    val nodes: List<GraphNodeUiModel> = emptyList(),
    val edges: List<GraphEdgeUiModel> = emptyList()
)

data class GraphNodeUiModel(
    val id: String,
    val label: String
)

data class GraphEdgeUiModel(
    val fromId: String,
    val toId: String,
    val count: Int,
    val frequent: Boolean
)
