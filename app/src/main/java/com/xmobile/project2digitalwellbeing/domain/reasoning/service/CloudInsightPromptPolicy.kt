package com.xmobile.project2digitalwellbeing.domain.reasoning.service

import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightRequest
import javax.inject.Inject

class CloudInsightPromptPolicy @Inject constructor() {

    fun buildSystemInstruction(): String {
        return "You are a digital wellbeing assistant. Use only grounded context, avoid inventing facts, avoid medical claims, and return calm plain text without markdown."
    }

    private fun determineTone(riskScore: Int): String {
        return when {
            riskScore >= 75 -> "Empathetic but firm. Acknowledge high usage gently and emphasize immediate, actionable steps to disconnect."
            riskScore in 40..74 -> "Informative and neutral. Highlight patterns objectively and suggest manageable adjustments."
            else -> "Positive and encouraging. Validate their balanced habits and encourage them to maintain this healthy state."
        }
    }

    fun buildGroundingSection(request: CloudInsightRequest): String {
        val context = request.groundedContext
        val evidenceLines = context.evidence.joinToString(separator = "\n") { evidence ->
            "- ${evidence["pattern"] ?: "UNKNOWN"} :: ${evidence["key"] ?: "key"} = ${evidence["value"] ?: "value"}"
        }
        val recommendationLines = context.recommendations.joinToString(separator = "\n") { item ->
            "- ${item["title"] ?: "Recommendation"}: ${item["description"] ?: ""}"
        }
        val fallbackSection = request.fallbackInsight?.let {
            "\nFallback insight:\n- title: ${it.title}\n- description: ${it.description}\n- suggestion: ${it.suggestion}"
        }.orEmpty()

        return buildString {
            appendLine("Summary: ${context.summary}")
            appendLine("Primary pattern: ${context.primaryPattern ?: "NONE"}")
            appendLine("Secondary patterns: ${context.secondaryPatterns.joinToString().ifBlank { "NONE" }}")
            appendLine("Risk score: ${context.riskScore}")
            appendLine("Required Tone: ${determineTone(context.riskScore)}")
            appendLine("Confidence: ${"%.2f".format(context.confidence)}")
            appendLine("Evidence:")
            appendLine(evidenceLines.ifBlank { "- none" })
            appendLine("Recommendations:")
            appendLine(recommendationLines.ifBlank { "- none" })
            append(fallbackSection)
        }.trim()
    }
}
