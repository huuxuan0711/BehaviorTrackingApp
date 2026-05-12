package com.xmobile.project2digitalwellbeing.presentation.analysis.transition

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase.GetTransitionGraphExperienceOutcome
import com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase.GetTransitionGraphExperienceUseCase
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppTransitionStat
import com.xmobile.project2digitalwellbeing.domain.tracking.model.TransitionFilter
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataOutcome
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataParams
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.AnalysisTimeRange
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetTransitionGraphDataParams
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
    application: Application,
    private val refreshUsageDataUseCase: RefreshUsageDataUseCase,
    private val getTransitionGraphExperienceUseCase: GetTransitionGraphExperienceUseCase
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()

    private val _uiState = MutableStateFlow(AppTransitionGraphUiState())
    val uiState: StateFlow<AppTransitionGraphUiState> = _uiState.asStateFlow()
    private var hasLoadedData = false

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
        val timezoneId = ZoneId.systemDefault().id
        val nowMillis = System.currentTimeMillis()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val refreshError = if (!hasLoadedData) {
                when (
                    val refreshOutcome = refreshUsageDataUseCase(
                        RefreshUsageDataParams(
                            nowMillis = nowMillis,
                            timezoneId = timezoneId,
                            forceFullRefresh = false
                        )
                    )
                ) {
                    is RefreshUsageDataOutcome.Success -> null
                    is RefreshUsageDataOutcome.Failure -> refreshOutcome.error.toUserMessage()
                }
            } else {
                null
            }

            when (
                val outcome = getTransitionGraphExperienceUseCase(
                    GetTransitionGraphDataParams(
                        nowMillis = nowMillis,
                        timezoneId = timezoneId,
                        timeRange = state.timeRange,
                        filter = state.filter
                    )
                )
            ) {
                is GetTransitionGraphExperienceOutcome.Success -> {
                    hasLoadedData = true
                    val dominantEdges = toDominantEdges(outcome.data.data.transitions)
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
                            errorMessage = refreshError,
                            nodes = nodes,
                            edges = filteredEdges,
                            insightText = refreshError ?: outcome.data.insightSummaryText
                        )
                    }
                }

                is GetTransitionGraphExperienceOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = refreshError ?: context.getString(R.string.auto_transition_data_unavailable),
                            nodes = emptyList(),
                            edges = emptyList(),
                            insightText = refreshError ?: context.getString(R.string.auto_no_transition_insight)
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

    private fun com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageDataError.toUserMessage(): String {
        return when (this) {
            is com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageDataError.PermissionDenied ->
                context.getString(R.string.auto_error_permission_denied)

            is com.xmobile.project2digitalwellbeing.domain.tracking.usecase.UsageDataError.InvalidTimeZone ->
                context.getString(R.string.auto_error_invalid_timezone)

            else -> context.getString(R.string.auto_error_refresh_failure)
        }
    }
}
