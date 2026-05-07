package com.xmobile.project2digitalwellbeing.presentation.analysis.transition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppTransitionStat
import com.xmobile.project2digitalwellbeing.domain.tracking.model.TransitionFilter
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.AnalysisTimeRange
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetTransitionGraphDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetTransitionGraphDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetTransitionGraphDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AppTransitionGraphViewModel @Inject constructor(
    private val getTransitionGraphDataUseCase: GetTransitionGraphDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppTransitionGraphUiState())
    val uiState: StateFlow<AppTransitionGraphUiState> = _uiState.asStateFlow()

    fun setTimeRange(timeRange: AnalysisTimeRange) {
        if (_uiState.value.timeRange == timeRange) return
        _uiState.update { it.copy(timeRange = timeRange) }
        load()
    }

    fun setFilter(filter: TransitionFilter) {
        if (_uiState.value.filter == filter) return
        _uiState.update { it.copy(filter = filter) }
        load()
    }

    fun load() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (
                val outcome = getTransitionGraphDataUseCase(
                    GetTransitionGraphDataParams(
                        nowMillis = System.currentTimeMillis(),
                        timezoneId = ZoneId.systemDefault().id,
                        timeRange = state.timeRange,
                        filter = state.filter
                    )
                )
            ) {
                is GetTransitionGraphDataOutcome.Success -> {
                    val dominantEdges = toDominantEdges(outcome.data.transitions)
                    val maxCount = dominantEdges.maxOfOrNull { it.transitionCount } ?: 0
                    val frequentThreshold = if (maxCount <= 1) 1 else kotlin.math.ceil(maxCount * 0.6).toInt()

                    val edges = dominantEdges
                        .sortedByDescending { it.transitionCount }
                        .take(MAX_EDGE_COUNT)

                    val nodeLabels = linkedMapOf<String, String>()
                    edges.forEach { edge ->
                        nodeLabels.putIfAbsent(edge.fromPackageName, edge.fromAppName ?: edge.fromPackageName)
                        nodeLabels.putIfAbsent(edge.toPackageName, edge.toAppName ?: edge.toPackageName)
                    }

                    val nodes = nodeLabels.entries
                        .take(MAX_NODE_COUNT)
                        .map { GraphNodeUiModel(id = it.key, label = it.value) }
                    val allowedNodeIds = nodes.map { it.id }.toHashSet()

                    val filteredEdges = edges
                        .filter { it.fromPackageName in allowedNodeIds && it.toPackageName in allowedNodeIds }
                        .map {
                            GraphEdgeUiModel(
                                fromId = it.fromPackageName,
                                toId = it.toPackageName,
                                count = it.transitionCount,
                                frequent = it.transitionCount >= frequentThreshold
                            )
                        }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            nodes = nodes,
                            edges = filteredEdges,
                            insightText = outcome.data.insight?.summary
                                ?: "No transition insight yet. Keep using apps to build pattern data."
                        )
                    }
                }

                is GetTransitionGraphDataOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Transition data is not available yet.",
                            nodes = emptyList(),
                            edges = emptyList(),
                            insightText = "No transition insight yet."
                        )
                    }
                }
            }
        }
    }

    private fun toDominantEdges(transitions: List<AppTransitionStat>): List<AppTransitionStat> {
        val byPair = linkedMapOf<String, AppTransitionStat>()
        transitions.forEach { transition ->
            val pairKey = if (transition.fromPackageName <= transition.toPackageName) {
                "${transition.fromPackageName}|${transition.toPackageName}"
            } else {
                "${transition.toPackageName}|${transition.fromPackageName}"
            }

            val current = byPair[pairKey]
            if (
                current == null ||
                transition.transitionCount > current.transitionCount ||
                (
                    transition.transitionCount == current.transitionCount &&
                        transition.lastTransitionTimestampMillis > current.lastTransitionTimestampMillis
                    )
            ) {
                byPair[pairKey] = transition
            }
        }
        return byPair.values.toList()
    }

    companion object {
        private const val MAX_NODE_COUNT = 7
        private const val MAX_EDGE_COUNT = 12
    }
}
