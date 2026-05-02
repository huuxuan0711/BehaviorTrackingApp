package com.xmobile.project2digitalwellbeing.domain.insights.service

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppFocusGroup
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppTransitionStat
import com.xmobile.project2digitalwellbeing.domain.tracking.model.TransitionFilter
import com.xmobile.project2digitalwellbeing.domain.insights.model.TransitionInsight
import javax.inject.Inject

class TransitionInsightGeneratorImpl @Inject constructor() : TransitionInsightGenerator {

    override fun filterTransitions(
        transitions: List<AppTransitionStat>,
        filter: TransitionFilter
    ): List<AppTransitionStat> {
        return transitions.filter { transition ->
            when (filter) {
                TransitionFilter.ALL -> true
                TransitionFilter.PRODUCTIVE -> {
                    transition.fromCategory.toFocusGroup() == AppFocusGroup.PRODUCTIVE &&
                        transition.toCategory.toFocusGroup() == AppFocusGroup.PRODUCTIVE
                }
                TransitionFilter.DISTRACTING -> {
                    transition.fromCategory.toFocusGroup() == AppFocusGroup.DISTRACTING &&
                        transition.toCategory.toFocusGroup() == AppFocusGroup.DISTRACTING
                }

                TransitionFilter.PRODUCTIVE_MIXED -> {
                    transition.fromCategory.toFocusGroup() == AppFocusGroup.PRODUCTIVE ||
                        transition.toCategory.toFocusGroup() == AppFocusGroup.PRODUCTIVE
                }

                TransitionFilter.DISTRACTING_MIXED -> {
                    transition.fromCategory.toFocusGroup() == AppFocusGroup.DISTRACTING ||
                        transition.toCategory.toFocusGroup() == AppFocusGroup.DISTRACTING
                }
            }
        }
    }

    override fun generateInsight(
        transitions: List<AppTransitionStat>,
        filter: TransitionFilter
    ): TransitionInsight? {
        val filteredTransitions = filterTransitions(transitions, filter)
        if (filteredTransitions.isEmpty()) {
            return null
        }

        val dominantTransition = filteredTransitions.maxWithOrNull(
            compareBy<AppTransitionStat> { it.transitionCount }
                .thenBy { it.lateNightTransitionCount }
                .thenBy { it.lastTransitionTimestampMillis }
        ) ?: return null

        val totalTransitionCount = filteredTransitions.sumOf { it.transitionCount }
        val totalLateNightTransitions = filteredTransitions.sumOf { it.lateNightTransitionCount }
        val totalIntervals = filteredTransitions.sumOf { it.totalIntervalMillis }
        val lateNightTransitionRatio = safeRatio(
            numerator = totalLateNightTransitions,
            denominator = totalTransitionCount
        )
        val score = scoreTransitions(
            dominantTransitionCount = dominantTransition.transitionCount,
            totalTransitionCount = totalTransitionCount,
            lateNightTransitionRatio = lateNightTransitionRatio,
            filter = filter
        )
        val confidence = confidenceTransitions(
            dominantTransitionCount = dominantTransition.transitionCount,
            totalTransitionCount = totalTransitionCount,
            filter = filter
        )

        return TransitionInsight(
            filter = filter,
            title = buildTitle(filter),
            summary = buildSummary(
                dominantTransition = dominantTransition,
                filter = filter,
                totalTransitionCount = totalTransitionCount,
                lateNightTransitionRatio = lateNightTransitionRatio
            ),
            score = score,
            confidence = confidence,
            totalTransitionCount = totalTransitionCount,
            lateNightTransitionRatio = lateNightTransitionRatio,
            averageIntervalMillis = if (totalTransitionCount == 0) 0L else totalIntervals / totalTransitionCount,
            dominantTransition = dominantTransition
        )
    }

    private fun buildTitle(filter: TransitionFilter): String {
        return when (filter) {
            TransitionFilter.ALL -> "Most common app switch"
            TransitionFilter.PRODUCTIVE -> "Most common productive switch"
            TransitionFilter.DISTRACTING -> "Most common distracting switch"
            TransitionFilter.PRODUCTIVE_MIXED -> "Productive-linked app switch"
            TransitionFilter.DISTRACTING_MIXED -> "Distracting-linked app switch"
        }
    }

    private fun buildSummary(
        dominantTransition: AppTransitionStat,
        filter: TransitionFilter,
        totalTransitionCount: Int,
        lateNightTransitionRatio: Float
    ): String {
        val fromLabel = dominantTransition.fromAppName ?: dominantTransition.fromPackageName
        val toLabel = dominantTransition.toAppName ?: dominantTransition.toPackageName
        val lateNightPercent = (lateNightTransitionRatio * 100f).toInt()

        return when (filter) {
            TransitionFilter.ALL -> {
                "$fromLabel to $toLabel appears most often with ${dominantTransition.transitionCount} switches out of $totalTransitionCount."
            }
            TransitionFilter.PRODUCTIVE -> {
                "$fromLabel to $toLabel is the strongest productive flow, accounting for ${dominantTransition.transitionCount} switches."
            }
            TransitionFilter.DISTRACTING -> {
                "$fromLabel to $toLabel leads the distracting graph, with $lateNightPercent% of filtered switches happening late at night."
            }

            TransitionFilter.PRODUCTIVE_MIXED -> {
                "$fromLabel to $toLabel is the strongest productive-linked path, showing up ${dominantTransition.transitionCount} times in the filtered graph."
            }

            TransitionFilter.DISTRACTING_MIXED -> {
                "$fromLabel to $toLabel is the strongest distracting-linked path, with $lateNightPercent% of filtered switches happening late at night."
            }
        }
    }

    private fun scoreTransitions(
        dominantTransitionCount: Int,
        totalTransitionCount: Int,
        lateNightTransitionRatio: Float,
        filter: TransitionFilter
    ): Int {
        val concentrationScore = safeRatio(
            numerator = dominantTransitionCount,
            denominator = totalTransitionCount
        ) * 60f
        val volumeScore = dominantTransitionCount.coerceAtMost(10) * 3f
        val lateNightBoost = when (filter) {
            TransitionFilter.DISTRACTING,
            TransitionFilter.DISTRACTING_MIXED -> lateNightTransitionRatio * 20f

            else -> 0f
        }

        return (concentrationScore + volumeScore + lateNightBoost).toInt().coerceIn(0, 100)
    }

    private fun confidenceTransitions(
        dominantTransitionCount: Int,
        totalTransitionCount: Int,
        filter: TransitionFilter
    ): Float {
        val enoughVolume = (totalTransitionCount / 12f).coerceIn(0f, 1f)
        val dominantWeight = (dominantTransitionCount / 6f).coerceIn(0f, 1f)
        val filterWeight = when (filter) {
            TransitionFilter.ALL -> 0.75f
            TransitionFilter.PRODUCTIVE,
            TransitionFilter.DISTRACTING -> 0.85f
            TransitionFilter.PRODUCTIVE_MIXED,
            TransitionFilter.DISTRACTING_MIXED -> 0.8f
        }

        return ((enoughVolume * 0.45f) + (dominantWeight * 0.35f) + (filterWeight * 0.2f))
            .coerceIn(0f, 1f)
    }

    private fun AppCategory.toFocusGroup(): AppFocusGroup {
        return when (this) {
            AppCategory.PRODUCTIVITY,
            AppCategory.EDUCATION,
            AppCategory.TOOLS -> AppFocusGroup.PRODUCTIVE

            AppCategory.SOCIAL,
            AppCategory.VIDEO,
            AppCategory.GAME,
            AppCategory.MUSIC -> AppFocusGroup.DISTRACTING

            AppCategory.COMMUNICATION,
            AppCategory.BROWSER,
            AppCategory.OTHER,
            AppCategory.UNKNOWN -> AppFocusGroup.NEUTRAL
        }
    }

    private fun safeRatio(numerator: Int, denominator: Int): Float {
        if (denominator <= 0) {
            return 0f
        }
        return numerator.toFloat() / denominator.toFloat()
    }
}
