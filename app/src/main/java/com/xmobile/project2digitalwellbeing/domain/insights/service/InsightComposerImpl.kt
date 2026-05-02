package com.xmobile.project2digitalwellbeing.domain.insights.service

import com.xmobile.project2digitalwellbeing.domain.insights.model.ComposedInsight
import com.xmobile.project2digitalwellbeing.domain.insights.model.Insight
import com.xmobile.project2digitalwellbeing.domain.insights.model.InsightType
import com.xmobile.project2digitalwellbeing.domain.tracking.model.TransitionFilter
import com.xmobile.project2digitalwellbeing.domain.insights.model.TransitionInsight
import javax.inject.Inject

class InsightComposerImpl @Inject constructor() : InsightComposer {

    override fun compose(
        usageInsights: List<Insight>,
        transitionInsight: TransitionInsight?
    ): List<ComposedInsight> {
        if (transitionInsight == null) {
            return emptyList()
        }

        val usageInsightTypes = usageInsights.map { it.type }.toSet()
        val composedInsights = mutableListOf<ComposedInsight>()

        if (
            InsightType.LATE_NIGHT_SWITCHING in usageInsightTypes &&
            transitionInsight.filter in setOf(
                TransitionFilter.DISTRACTING,
                TransitionFilter.DISTRACTING_MIXED
            ) &&
            transitionInsight.lateNightTransitionRatio >= 0.4f
        ) {
            composedInsights += ComposedInsight(
                title = "Late-night distracting switching",
                summary = buildLateNightSummary(transitionInsight),
                score = ((transitionInsight.score * 0.6f) + 35f).toInt().coerceIn(0, 100),
                confidence = ((transitionInsight.confidence * 0.7f) + 0.2f).coerceIn(0f, 1f),
                sourceInsightTypes = listOf(
                    InsightType.LATE_NIGHT_SWITCHING,
                    InsightType.FREQUENT_SWITCHING
                ).filter { it in usageInsightTypes },
                transitionFilter = transitionInsight.filter,
                relatedPackages = listOf(
                    transitionInsight.dominantTransition.fromPackageName,
                    transitionInsight.dominantTransition.toPackageName
                ).distinct()
            )
        }

        if (
            InsightType.FREQUENT_SWITCHING in usageInsightTypes &&
            transitionInsight.totalTransitionCount >= 8 &&
            transitionInsight.dominantTransition.transitionCount >= 3
        ) {
            composedInsights += ComposedInsight(
                title = "Repeated switch loop",
                summary = buildRepeatedLoopSummary(transitionInsight),
                score = ((transitionInsight.score * 0.65f) + 25f).toInt().coerceIn(0, 100),
                confidence = ((transitionInsight.confidence * 0.75f) + 0.15f).coerceIn(0f, 1f),
                sourceInsightTypes = listOf(InsightType.FREQUENT_SWITCHING),
                transitionFilter = transitionInsight.filter,
                relatedPackages = listOf(
                    transitionInsight.dominantTransition.fromPackageName,
                    transitionInsight.dominantTransition.toPackageName
                ).distinct()
            )
        }

        return composedInsights
            .distinctBy { it.title to it.transitionFilter }
            .sortedByDescending { it.score }
    }

    private fun buildLateNightSummary(transitionInsight: TransitionInsight): String {
        val fromLabel = transitionInsight.dominantTransition.fromAppName
            ?: transitionInsight.dominantTransition.fromPackageName
        val toLabel = transitionInsight.dominantTransition.toAppName
            ?: transitionInsight.dominantTransition.toPackageName
        val ratioPercent = (transitionInsight.lateNightTransitionRatio * 100f).toInt()
        return "$fromLabel to $toLabel dominates your late-night switching pattern, with $ratioPercent% of those filtered transitions happening after hours."
    }

    private fun buildRepeatedLoopSummary(transitionInsight: TransitionInsight): String {
        val fromLabel = transitionInsight.dominantTransition.fromAppName
            ?: transitionInsight.dominantTransition.fromPackageName
        val toLabel = transitionInsight.dominantTransition.toAppName
            ?: transitionInsight.dominantTransition.toPackageName
        return "$fromLabel to $toLabel appears repeatedly in your switching graph, suggesting a stable loop rather than isolated app checks."
    }
}
