package com.xmobile.project2digitalwellbeing.domain.reasoning.service

import com.xmobile.project2digitalwellbeing.domain.insights.model.InsightType
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.BehaviorEvidence
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.BehaviorHypothesis
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.BehaviorPatternType
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.BehaviorReasoningInput
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.BehaviorReasoningResult
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.InterventionRecommendation
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.LlmGroundedContext
import javax.inject.Inject
import kotlin.math.max

class BehaviorReasoningEngineImpl @Inject constructor() : BehaviorReasoningEngine {

    override fun reason(input: BehaviorReasoningInput): BehaviorReasoningResult {
        val hypotheses = listOfNotNull(
            lateNightDrift(input),
            fragmentedAttention(input),
            compulsiveChecking(input),
            workHourLeakage(input),
            appDominance(input)
        ).sortedWith(
            compareByDescending<BehaviorHypothesis> { it.riskScore }
                .thenByDescending { it.confidence }
        )

        val primary = hypotheses.firstOrNull()
        val secondary = hypotheses.drop(1).take(MAX_SECONDARY_HYPOTHESES)
        val recommendations = buildRecommendations(primary, secondary)
        val summary = buildSummary(primary, secondary)

        return BehaviorReasoningResult(
            primaryHypothesis = primary,
            secondaryHypotheses = secondary,
            recommendations = recommendations,
            summary = summary,
            llmContext = buildLlmContext(primary, secondary, recommendations, summary)
        )
    }

    private fun lateNightDrift(input: BehaviorReasoningInput): BehaviorHypothesis? {
        val ratio = input.features.lateNightUsageRatio
        val baselineDelta = ratio - input.baseline.averageLateNightUsageRatio
        val hasInsight = input.usageInsights.any { it.type == InsightType.LATE_NIGHT_USAGE }
        if (ratio < LATE_NIGHT_RATIO_THRESHOLD && baselineDelta < LATE_NIGHT_BASELINE_DELTA_THRESHOLD && !hasInsight) {
            return null
        }

        val evidence = mutableListOf(
            BehaviorEvidence("lateNightUsageRatio", ratio.formatRatio(), 0.45f),
            BehaviorEvidence("lateNightSessionCount", input.features.lateNightSessionCount.toString(), 0.25f),
            BehaviorEvidence("baselineDelta", baselineDelta.formatSignedRatio(), 0.30f)
        )
        input.features.peakUsageHour?.let { peakHour ->
            evidence += BehaviorEvidence("peakUsageHour", peakHour.toString(), 0.20f)
        }
        val riskScore = weightedRiskScore(
            ratioWeight = normalize(ratio, 0.20f, 0.70f),
            baselineWeight = normalize(baselineDelta, 0.05f, 0.30f),
            insightWeight = if (hasInsight) 0.2f else 0f
        )
        return BehaviorHypothesis(
            pattern = BehaviorPatternType.LATE_NIGHT_DRIFT,
            summary = "Late-night usage is elevated and drifting above your normal pattern.",
            evidence = evidence,
            riskScore = riskScore,
            confidence = confidenceFrom(riskScore, hasInsight)
        )
    }

    private fun fragmentedAttention(input: BehaviorReasoningInput): BehaviorHypothesis? {
        val switchDensity = input.features.switchesPerHour
        val shortRatio = input.features.sessionLengthDistribution.shortSessionRatio
        val baselineSwitchDelta = switchDensity - input.baseline.averageSwitchesPerHour
        val hasInsight = input.usageInsights.any { it.type == InsightType.FREQUENT_SWITCHING } ||
            input.usageInsights.any { it.type == InsightType.LATE_NIGHT_SWITCHING }
        val hasTransitionSignal = input.transitionInsight?.totalTransitionCount ?: 0 >= MIN_TRANSITIONS_FOR_FRAGMENTATION
        if (
            switchDensity < SWITCHES_PER_HOUR_THRESHOLD &&
            shortRatio < SHORT_SESSION_RATIO_THRESHOLD &&
            baselineSwitchDelta < BASELINE_SWITCH_DELTA_THRESHOLD &&
            !hasInsight
        ) {
            return null
        }

        val evidence = listOf(
            BehaviorEvidence("switchesPerHour", switchDensity.formatRatio(), 0.45f),
            BehaviorEvidence("shortSessionRatio", shortRatio.formatRatio(), 0.30f),
            BehaviorEvidence("baselineSwitchDelta", baselineSwitchDelta.formatSignedRatio(), 0.25f)
        )
        val riskScore = weightedRiskScore(
            ratioWeight = normalize(switchDensity, 8f, 30f),
            baselineWeight = normalize(baselineSwitchDelta, 1.5f, 10f),
            insightWeight = if (hasInsight || hasTransitionSignal) 0.2f else 0f
        )
        return BehaviorHypothesis(
            pattern = BehaviorPatternType.FRAGMENTED_ATTENTION,
            summary = "Frequent context switching indicates fragmented attention.",
            evidence = evidence,
            riskScore = riskScore,
            confidence = confidenceFrom(riskScore, hasInsight || hasTransitionSignal)
        )
    }

    private fun compulsiveChecking(input: BehaviorReasoningInput): BehaviorHypothesis? {
        val shortRatio = input.features.sessionLengthDistribution.shortSessionRatio
        val sessionCount = input.features.totalSessionCount
        val baselineShortDelta = shortRatio - input.baseline.averageShortSessionRatio
        val hasInsight = input.usageInsights.any { it.type == InsightType.CONSTANT_CHECKING }
        if (
            shortRatio < SHORT_SESSION_RATIO_THRESHOLD &&
            sessionCount < MIN_SESSION_COUNT_FOR_CHECKING &&
            baselineShortDelta < SHORT_RATIO_BASELINE_DELTA_THRESHOLD &&
            !hasInsight
        ) {
            return null
        }

        val evidence = listOf(
            BehaviorEvidence("shortSessionRatio", shortRatio.formatRatio(), 0.45f),
            BehaviorEvidence("sessionCount", sessionCount.toString(), 0.30f),
            BehaviorEvidence("baselineShortDelta", baselineShortDelta.formatSignedRatio(), 0.25f)
        )
        val riskScore = weightedRiskScore(
            ratioWeight = normalize(shortRatio, 0.5f, 0.95f),
            baselineWeight = normalize(baselineShortDelta, 0.08f, 0.35f),
            insightWeight = if (hasInsight) 0.2f else 0f
        )
        return BehaviorHypothesis(
            pattern = BehaviorPatternType.COMPULSIVE_CHECKING,
            summary = "Short and repeated sessions suggest a checking loop.",
            evidence = evidence,
            riskScore = riskScore,
            confidence = confidenceFrom(riskScore, hasInsight)
        )
    }

    private fun workHourLeakage(input: BehaviorReasoningInput): BehaviorHypothesis? {
        val distractionMillis = input.features.workHourDistractionMillis
        val baselineDelta = distractionMillis - input.baseline.averageWorkHourDistractionMillis
        val hasInsight = input.usageInsights.any { it.type == InsightType.WORK_HOUR_DISTRACTION }
        if (
            distractionMillis < WORK_HOUR_DISTRACTION_THRESHOLD_MILLIS &&
            baselineDelta < WORK_HOUR_BASELINE_DELTA_THRESHOLD_MILLIS &&
            !hasInsight
        ) {
            return null
        }

        val evidence = listOf(
            BehaviorEvidence("workHourDistractionMillis", distractionMillis.toString(), 0.50f),
            BehaviorEvidence("baselineDistractionDeltaMillis", baselineDelta.toString(), 0.30f),
            BehaviorEvidence("distractingTopApps", input.features.workHourTopApps.take(3).size.toString(), 0.20f)
        )
        val riskScore = weightedRiskScore(
            ratioWeight = normalize(distractionMillis.toFloat(), (30L * MINUTE_MILLIS).toFloat(), (3L * HOUR_MILLIS).toFloat()),
            baselineWeight = normalize(baselineDelta.toFloat(), (15L * MINUTE_MILLIS).toFloat(), (2L * HOUR_MILLIS).toFloat()),
            insightWeight = if (hasInsight) 0.2f else 0f
        )
        return BehaviorHypothesis(
            pattern = BehaviorPatternType.WORK_HOUR_LEAKAGE,
            summary = "Distracting app usage during work hours is above a healthy limit.",
            evidence = evidence,
            riskScore = riskScore,
            confidence = confidenceFrom(riskScore, hasInsight)
        )
    }

    private fun appDominance(input: BehaviorReasoningInput): BehaviorHypothesis? {
        val topApp = input.features.topAppsByDuration.firstOrNull() ?: return null
        if (input.features.totalScreenTimeMillis <= 0L) return null
        val topShare = topApp.totalTimeMillis.toFloat() / input.features.totalScreenTimeMillis.toFloat()
        val baselineDelta = topShare - input.baseline.averageTopAppShare
        val hasInsight = input.usageInsights.any { it.type == InsightType.APP_RELIANCE }
        if (topShare < APP_DOMINANCE_SHARE_THRESHOLD && baselineDelta < APP_DOMINANCE_BASELINE_DELTA_THRESHOLD && !hasInsight) {
            return null
        }

        val evidence = listOf(
            BehaviorEvidence("dominantApp", topApp.appName ?: topApp.packageName, 0.20f),
            BehaviorEvidence("dominantShare", topShare.formatRatio(), 0.55f),
            BehaviorEvidence("baselineShareDelta", baselineDelta.formatSignedRatio(), 0.25f)
        )
        val riskScore = weightedRiskScore(
            ratioWeight = normalize(topShare, 0.40f, 0.90f),
            baselineWeight = normalize(baselineDelta, 0.05f, 0.35f),
            insightWeight = if (hasInsight) 0.2f else 0f
        )
        return BehaviorHypothesis(
            pattern = BehaviorPatternType.APP_DOMINANCE,
            summary = "A single app dominates your screen-time budget.",
            evidence = evidence,
            riskScore = riskScore,
            confidence = confidenceFrom(riskScore, hasInsight)
        )
    }

    private fun buildRecommendations(
        primary: BehaviorHypothesis?,
        secondary: List<BehaviorHypothesis>
    ): List<InterventionRecommendation> {
        val uniquePatterns = listOfNotNull(primary?.pattern) + secondary.map { it.pattern }
        return uniquePatterns.distinct().mapIndexed { index, pattern ->
            when (pattern) {
                BehaviorPatternType.LATE_NIGHT_DRIFT -> InterventionRecommendation(
                    title = "Start wind-down mode",
                    description = "Enable bedtime reminder and reduce stimulation after 10 PM.",
                    suggestedTimeLabel = "21:30-22:00",
                    priority = 100 - index
                )
                BehaviorPatternType.FRAGMENTED_ATTENTION -> InterventionRecommendation(
                    title = "Run focus blocks",
                    description = "Batch tasks and avoid rapid app switching during focus periods.",
                    suggestedTimeLabel = "Morning work blocks",
                    priority = 100 - index
                )
                BehaviorPatternType.COMPULSIVE_CHECKING -> InterventionRecommendation(
                    title = "Batch notification checks",
                    description = "Check notifications on schedule instead of impulse checks.",
                    suggestedTimeLabel = "Every 60-90 minutes",
                    priority = 100 - index
                )
                BehaviorPatternType.WORK_HOUR_LEAKAGE -> InterventionRecommendation(
                    title = "Use work-hour app limits",
                    description = "Restrict distracting categories during work sessions.",
                    suggestedTimeLabel = "09:00-17:00",
                    priority = 100 - index
                )
                BehaviorPatternType.APP_DOMINANCE -> InterventionRecommendation(
                    title = "Set single-app quota",
                    description = "Cap daily time for the dominant app and diversify activities.",
                    suggestedTimeLabel = "At first launch",
                    priority = 100 - index
                )
            }
        }
    }

    private fun buildSummary(
        primary: BehaviorHypothesis?,
        secondary: List<BehaviorHypothesis>
    ): String {
        if (primary == null) return "No high-risk behavior pattern detected in the current window."
        if (secondary.isEmpty()) return primary.summary
        return "${primary.summary} ${secondary.take(1).joinToString(" ") { it.summary }}"
    }

    private fun buildLlmContext(
        primary: BehaviorHypothesis?,
        secondary: List<BehaviorHypothesis>,
        recommendations: List<InterventionRecommendation>,
        summary: String
    ): LlmGroundedContext {
        val evidence = listOfNotNull(primary) + secondary
        return LlmGroundedContext(
            primaryPattern = primary?.pattern?.name,
            secondaryPatterns = secondary.map { it.pattern.name },
            riskScore = primary?.riskScore ?: 0,
            confidence = primary?.confidence ?: 0f,
            summary = summary,
            evidence = evidence.flatMap { hypothesis ->
                hypothesis.evidence.map { item ->
                    mapOf(
                        "pattern" to hypothesis.pattern.name,
                        "key" to item.key,
                        "value" to item.value
                    )
                }
            },
            recommendations = recommendations.map {
                mapOf(
                    "title" to it.title,
                    "description" to it.description,
                    "suggestedTimeLabel" to it.suggestedTimeLabel,
                    "priority" to it.priority.toString()
                )
            }
        )
    }

    private fun weightedRiskScore(ratioWeight: Float, baselineWeight: Float, insightWeight: Float): Int {
        val weighted = (ratioWeight * 0.6f) + (baselineWeight * 0.3f) + insightWeight
        return (weighted * 100f).toInt().coerceIn(0, 100)
    }

    private fun confidenceFrom(riskScore: Int, hasInsightSupport: Boolean): Float {
        val base = max(riskScore, MIN_CONFIDENCE_RISK_SCORE).toFloat() / 100f
        return (if (hasInsightSupport) base + 0.1f else base).coerceIn(0.45f, 0.98f)
    }

    private fun normalize(value: Float, min: Float, max: Float): Float {
        if (max <= min) return 0f
        return ((value - min) / (max - min)).coerceIn(0f, 1f)
    }

    private fun Float.formatRatio(): String = String.format("%.2f", this)

    private fun Float.formatSignedRatio(): String = if (this >= 0f) {
        "+${formatRatio()}"
    } else {
        formatRatio()
    }

    private companion object {
        private const val MAX_SECONDARY_HYPOTHESES = 2
        private const val MIN_CONFIDENCE_RISK_SCORE = 45

        private const val HOUR_MILLIS = 60L * 60L * 1000L
        private const val MINUTE_MILLIS = 60L * 1000L

        private const val LATE_NIGHT_RATIO_THRESHOLD = 0.25f
        private const val LATE_NIGHT_BASELINE_DELTA_THRESHOLD = 0.08f

        private const val SWITCHES_PER_HOUR_THRESHOLD = 10f
        private const val BASELINE_SWITCH_DELTA_THRESHOLD = 2f
        private const val SHORT_SESSION_RATIO_THRESHOLD = 0.60f
        private const val MIN_TRANSITIONS_FOR_FRAGMENTATION = 8

        private const val MIN_SESSION_COUNT_FOR_CHECKING = 15
        private const val SHORT_RATIO_BASELINE_DELTA_THRESHOLD = 0.12f

        private const val WORK_HOUR_DISTRACTION_THRESHOLD_MILLIS = 45L * MINUTE_MILLIS
        private const val WORK_HOUR_BASELINE_DELTA_THRESHOLD_MILLIS = 20L * MINUTE_MILLIS

        private const val APP_DOMINANCE_SHARE_THRESHOLD = 0.50f
        private const val APP_DOMINANCE_BASELINE_DELTA_THRESHOLD = 0.10f
    }
}
