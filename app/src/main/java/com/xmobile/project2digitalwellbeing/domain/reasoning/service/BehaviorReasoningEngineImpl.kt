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
        val recommendations = buildRecommendations(primary, secondary, input.preferences.languageCode)
        val summary = buildSummary(primary, secondary, input.preferences.languageCode)

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
            summary = summaryText(input.preferences.languageCode, BehaviorPatternType.LATE_NIGHT_DRIFT),
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
            summary = summaryText(input.preferences.languageCode, BehaviorPatternType.FRAGMENTED_ATTENTION),
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
            summary = summaryText(input.preferences.languageCode, BehaviorPatternType.COMPULSIVE_CHECKING),
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
            summary = summaryText(input.preferences.languageCode, BehaviorPatternType.WORK_HOUR_LEAKAGE),
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
            summary = summaryText(input.preferences.languageCode, BehaviorPatternType.APP_DOMINANCE),
            evidence = evidence,
            riskScore = riskScore,
            confidence = confidenceFrom(riskScore, hasInsight)
        )
    }

    private fun buildRecommendations(
        primary: BehaviorHypothesis?,
        secondary: List<BehaviorHypothesis>,
        languageCode: String
    ): List<InterventionRecommendation> {
        val uniquePatterns = listOfNotNull(primary?.pattern) + secondary.map { it.pattern }
        return uniquePatterns.distinct().mapIndexed { index, pattern ->
            when (pattern) {
                BehaviorPatternType.LATE_NIGHT_DRIFT -> InterventionRecommendation(
                    title = recommendationTitle(languageCode, pattern),
                    description = recommendationDescription(languageCode, pattern),
                    suggestedTimeLabel = recommendationTimeLabel(languageCode, pattern),
                    priority = 100 - index
                )
                BehaviorPatternType.FRAGMENTED_ATTENTION -> InterventionRecommendation(
                    title = recommendationTitle(languageCode, pattern),
                    description = recommendationDescription(languageCode, pattern),
                    suggestedTimeLabel = recommendationTimeLabel(languageCode, pattern),
                    priority = 100 - index
                )
                BehaviorPatternType.COMPULSIVE_CHECKING -> InterventionRecommendation(
                    title = recommendationTitle(languageCode, pattern),
                    description = recommendationDescription(languageCode, pattern),
                    suggestedTimeLabel = recommendationTimeLabel(languageCode, pattern),
                    priority = 100 - index
                )
                BehaviorPatternType.WORK_HOUR_LEAKAGE -> InterventionRecommendation(
                    title = recommendationTitle(languageCode, pattern),
                    description = recommendationDescription(languageCode, pattern),
                    suggestedTimeLabel = recommendationTimeLabel(languageCode, pattern),
                    priority = 100 - index
                )
                BehaviorPatternType.APP_DOMINANCE -> InterventionRecommendation(
                    title = recommendationTitle(languageCode, pattern),
                    description = recommendationDescription(languageCode, pattern),
                    suggestedTimeLabel = recommendationTimeLabel(languageCode, pattern),
                    priority = 100 - index
                )
            }
        }
    }

    private fun buildSummary(
        primary: BehaviorHypothesis?,
        secondary: List<BehaviorHypothesis>,
        languageCode: String
    ): String {
        if (primary == null) return noRiskSummary(languageCode)
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

    private fun summaryText(languageCode: String, pattern: BehaviorPatternType): String {
        return when (languageCode.lowercase()) {
            "vi" -> when (pattern) {
                BehaviorPatternType.LATE_NIGHT_DRIFT -> "Việc sử dụng đêm khuya của bạn đang tăng và vượt lên trên mức bình thường."
                BehaviorPatternType.FRAGMENTED_ATTENTION -> "Việc chuyển đổi ngữ cảnh thường xuyên cho thấy sự tập trung đang bị phân mảnh."
                BehaviorPatternType.COMPULSIVE_CHECKING -> "Các phiên ngắn lặp lại cho thấy xu hướng kiểm tra điện thoại theo vòng lặp."
                BehaviorPatternType.WORK_HOUR_LEAKAGE -> "Việc dùng ứng dụng gây xao nhãng trong giờ làm việc đang vượt ngưỡng lành mạnh."
                BehaviorPatternType.APP_DOMINANCE -> "Một ứng dụng đang chiếm phần lớn ngân sách thời gian màn hình của bạn."
            }
            "fr" -> when (pattern) {
                BehaviorPatternType.LATE_NIGHT_DRIFT -> "Votre usage tardif augmente et dépasse votre niveau habituel."
                BehaviorPatternType.FRAGMENTED_ATTENTION -> "Les changements fréquents de contexte indiquent une attention fragmentée."
                BehaviorPatternType.COMPULSIVE_CHECKING -> "Des sessions courtes et répétées suggèrent une boucle de vérification."
                BehaviorPatternType.WORK_HOUR_LEAKAGE -> "L'usage d'apps distrayantes pendant le travail dépasse une limite saine."
                BehaviorPatternType.APP_DOMINANCE -> "Une seule application domine votre budget de temps d'écran."
            }
            "de" -> when (pattern) {
                BehaviorPatternType.LATE_NIGHT_DRIFT -> "Ihre späte Nutzung steigt an und liegt über Ihrem normalen Muster."
                BehaviorPatternType.FRAGMENTED_ATTENTION -> "Häufige Kontextwechsel deuten auf fragmentierte Aufmerksamkeit hin."
                BehaviorPatternType.COMPULSIVE_CHECKING -> "Kurze, wiederholte Sitzungen deuten auf eine Prüfschleife hin."
                BehaviorPatternType.WORK_HOUR_LEAKAGE -> "Ablenkende App-Nutzung während der Arbeitszeit liegt über einem gesunden Maß."
                BehaviorPatternType.APP_DOMINANCE -> "Eine einzelne App dominiert Ihr Bildschirmzeitbudget."
            }
            else -> when (pattern) {
                BehaviorPatternType.LATE_NIGHT_DRIFT -> "Late-night usage is elevated and drifting above your normal pattern."
                BehaviorPatternType.FRAGMENTED_ATTENTION -> "Frequent context switching indicates fragmented attention."
                BehaviorPatternType.COMPULSIVE_CHECKING -> "Short and repeated sessions suggest a checking loop."
                BehaviorPatternType.WORK_HOUR_LEAKAGE -> "Distracting app usage during work hours is above a healthy limit."
                BehaviorPatternType.APP_DOMINANCE -> "A single app dominates your screen-time budget."
            }
        }
    }

    private fun recommendationTitle(languageCode: String, pattern: BehaviorPatternType): String {
        return when (languageCode.lowercase()) {
            "vi" -> when (pattern) {
                BehaviorPatternType.LATE_NIGHT_DRIFT -> "Bắt đầu chế độ thư giãn"
                BehaviorPatternType.FRAGMENTED_ATTENTION -> "Tạo khối tập trung"
                BehaviorPatternType.COMPULSIVE_CHECKING -> "Gom lịch kiểm tra thông báo"
                BehaviorPatternType.WORK_HOUR_LEAKAGE -> "Đặt giới hạn ứng dụng giờ làm"
                BehaviorPatternType.APP_DOMINANCE -> "Đặt hạn mức cho một ứng dụng"
            }
            "fr" -> when (pattern) {
                BehaviorPatternType.LATE_NIGHT_DRIFT -> "Activer le mode détente"
                BehaviorPatternType.FRAGMENTED_ATTENTION -> "Créer des blocs de concentration"
                BehaviorPatternType.COMPULSIVE_CHECKING -> "Regrouper les vérifications"
                BehaviorPatternType.WORK_HOUR_LEAKAGE -> "Limiter les apps au travail"
                BehaviorPatternType.APP_DOMINANCE -> "Définir un quota d'application"
            }
            "de" -> when (pattern) {
                BehaviorPatternType.LATE_NIGHT_DRIFT -> "Entspannungsmodus starten"
                BehaviorPatternType.FRAGMENTED_ATTENTION -> "Fokusblöcke nutzen"
                BehaviorPatternType.COMPULSIVE_CHECKING -> "Benachrichtigungen bündeln"
                BehaviorPatternType.WORK_HOUR_LEAKAGE -> "App-Limits für Arbeitszeit"
                BehaviorPatternType.APP_DOMINANCE -> "Einzel-App-Limit setzen"
            }
            else -> when (pattern) {
                BehaviorPatternType.LATE_NIGHT_DRIFT -> "Start wind-down mode"
                BehaviorPatternType.FRAGMENTED_ATTENTION -> "Run focus blocks"
                BehaviorPatternType.COMPULSIVE_CHECKING -> "Batch notification checks"
                BehaviorPatternType.WORK_HOUR_LEAKAGE -> "Use work-hour app limits"
                BehaviorPatternType.APP_DOMINANCE -> "Set single-app quota"
            }
        }
    }

    private fun recommendationDescription(languageCode: String, pattern: BehaviorPatternType): String {
        return when (languageCode.lowercase()) {
            "vi" -> when (pattern) {
                BehaviorPatternType.LATE_NIGHT_DRIFT -> "Bật nhắc giờ ngủ và giảm kích thích sau 10 giờ tối."
                BehaviorPatternType.FRAGMENTED_ATTENTION -> "Gom việc thành cụm và tránh chuyển ứng dụng liên tục trong lúc cần tập trung."
                BehaviorPatternType.COMPULSIVE_CHECKING -> "Kiểm tra thông báo theo lịch thay vì mở máy theo phản xạ."
                BehaviorPatternType.WORK_HOUR_LEAKAGE -> "Hạn chế các nhóm ứng dụng gây xao nhãng trong phiên làm việc."
                BehaviorPatternType.APP_DOMINANCE -> "Giới hạn thời gian mỗi ngày cho ứng dụng chiếm ưu thế và đa dạng hóa hoạt động."
            }
            "fr" -> when (pattern) {
                BehaviorPatternType.LATE_NIGHT_DRIFT -> "Activez un rappel du coucher et réduisez la stimulation après 22 h."
                BehaviorPatternType.FRAGMENTED_ATTENTION -> "Regroupez les tâches et évitez les changements rapides d'application pendant les périodes de concentration."
                BehaviorPatternType.COMPULSIVE_CHECKING -> "Consultez les notifications à des horaires fixes au lieu de vérifier par impulsion."
                BehaviorPatternType.WORK_HOUR_LEAKAGE -> "Limitez les catégories distrayantes pendant les sessions de travail."
                BehaviorPatternType.APP_DOMINANCE -> "Fixez une limite quotidienne pour l'application dominante et variez vos activités."
            }
            "de" -> when (pattern) {
                BehaviorPatternType.LATE_NIGHT_DRIFT -> "Aktivieren Sie eine Schlafenszeit-Erinnerung und reduzieren Sie Reize nach 22 Uhr."
                BehaviorPatternType.FRAGMENTED_ATTENTION -> "Bündeln Sie Aufgaben und vermeiden Sie schnelle App-Wechsel in Fokusphasen."
                BehaviorPatternType.COMPULSIVE_CHECKING -> "Prüfen Sie Benachrichtigungen nach Plan statt aus Impuls."
                BehaviorPatternType.WORK_HOUR_LEAKAGE -> "Begrenzen Sie ablenkende Kategorien während der Arbeitszeit."
                BehaviorPatternType.APP_DOMINANCE -> "Begrenzen Sie die tägliche Zeit für die dominante App und variieren Sie Ihre Aktivitäten."
            }
            else -> when (pattern) {
                BehaviorPatternType.LATE_NIGHT_DRIFT -> "Enable bedtime reminder and reduce stimulation after 10 PM."
                BehaviorPatternType.FRAGMENTED_ATTENTION -> "Batch tasks and avoid rapid app switching during focus periods."
                BehaviorPatternType.COMPULSIVE_CHECKING -> "Check notifications on schedule instead of impulse checks."
                BehaviorPatternType.WORK_HOUR_LEAKAGE -> "Restrict distracting categories during work sessions."
                BehaviorPatternType.APP_DOMINANCE -> "Cap daily time for the dominant app and diversify activities."
            }
        }
    }

    private fun recommendationTimeLabel(languageCode: String, pattern: BehaviorPatternType): String {
        return when (languageCode.lowercase()) {
            "vi" -> when (pattern) {
                BehaviorPatternType.LATE_NIGHT_DRIFT -> "21:30-22:00"
                BehaviorPatternType.FRAGMENTED_ATTENTION -> "Các khối làm việc buổi sáng"
                BehaviorPatternType.COMPULSIVE_CHECKING -> "Mỗi 60-90 phút"
                BehaviorPatternType.WORK_HOUR_LEAKAGE -> "09:00-17:00"
                BehaviorPatternType.APP_DOMINANCE -> "Ngay lần mở đầu tiên"
            }
            "fr" -> when (pattern) {
                BehaviorPatternType.LATE_NIGHT_DRIFT -> "21:30-22:00"
                BehaviorPatternType.FRAGMENTED_ATTENTION -> "Blocs de travail du matin"
                BehaviorPatternType.COMPULSIVE_CHECKING -> "Toutes les 60-90 minutes"
                BehaviorPatternType.WORK_HOUR_LEAKAGE -> "09:00-17:00"
                BehaviorPatternType.APP_DOMINANCE -> "Au premier lancement"
            }
            "de" -> when (pattern) {
                BehaviorPatternType.LATE_NIGHT_DRIFT -> "21:30-22:00"
                BehaviorPatternType.FRAGMENTED_ATTENTION -> "Arbeitsblöcke am Morgen"
                BehaviorPatternType.COMPULSIVE_CHECKING -> "Alle 60-90 Minuten"
                BehaviorPatternType.WORK_HOUR_LEAKAGE -> "09:00-17:00"
                BehaviorPatternType.APP_DOMINANCE -> "Beim ersten Start"
            }
            else -> when (pattern) {
                BehaviorPatternType.LATE_NIGHT_DRIFT -> "21:30-22:00"
                BehaviorPatternType.FRAGMENTED_ATTENTION -> "Morning work blocks"
                BehaviorPatternType.COMPULSIVE_CHECKING -> "Every 60-90 minutes"
                BehaviorPatternType.WORK_HOUR_LEAKAGE -> "09:00-17:00"
                BehaviorPatternType.APP_DOMINANCE -> "At first launch"
            }
        }
    }

    private fun noRiskSummary(languageCode: String): String {
        return when (languageCode.lowercase()) {
            "vi" -> "Không phát hiện mẫu hành vi rủi ro cao trong khung thời gian hiện tại."
            "fr" -> "Aucun comportement à haut risque n'a été détecté dans cette période."
            "de" -> "Im aktuellen Zeitraum wurde kein Verhaltensmuster mit hohem Risiko erkannt."
            else -> "No high-risk behavior pattern detected in the current window."
        }
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
