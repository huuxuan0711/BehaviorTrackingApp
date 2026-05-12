package com.xmobile.project2digitalwellbeing.domain.reasoning.service

import com.xmobile.project2digitalwellbeing.domain.insights.service.InterpretedInsight
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.BehaviorReasoningResult
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.InsightResolutionDecision
import javax.inject.Inject

class LocalInsightNarratorImpl @Inject constructor() : LocalInsightNarrator {

    override fun narrate(
        resolutionDecision: InsightResolutionDecision,
        reasoningResult: BehaviorReasoningResult?,
        fallbackInsight: InterpretedInsight?,
        languageCode: String
    ): String {
        val reasoning = reasoningResult?.primaryHypothesis
        if (resolutionDecision.useLocalReasoning && reasoning != null) {
            val recommendation = reasoningResult.recommendations.firstOrNull()?.description
            return buildString {
                append(reasoningResult.summary)
                if (!recommendation.isNullOrBlank()) {
                    append(" ")
                    append(recommendation)
                }
            }.trim()
        }

        if (resolutionDecision.useRuleInsight && fallbackInsight != null) {
            return "${fallbackInsight.description} ${fallbackInsight.suggestion}".trim()
        }

        return defaultEmptyText(languageCode)
    }

    private fun defaultEmptyText(languageCode: String): String {
        return when (languageCode.lowercase()) {
            "vi" -> "Chưa có xu hướng rõ ràng. Hãy dùng điện thoại bình thường rồi kéo để làm mới."
            "fr" -> "Aucune tendance claire pour le moment. Utilisez votre téléphone normalement puis actualisez."
            "de" -> "Noch kein klares Muster. Nutzen Sie Ihr Telefon normal und aktualisieren Sie dann die Ansicht."
            else -> "No clear pattern yet. Use your phone normally, then pull to refresh."
        }
    }
}
