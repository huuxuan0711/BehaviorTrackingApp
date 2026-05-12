package com.xmobile.project2digitalwellbeing.domain.reasoning.service

import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightRequest
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightSurface
import javax.inject.Inject

class DashboardCloudInsightPromptBuilder @Inject constructor(
    private val policy: CloudInsightPromptPolicy
) : CloudInsightPromptBuilder {

    override fun supports(request: CloudInsightRequest): Boolean {
        return request.surface == CloudInsightSurface.DASHBOARD ||
            request.surface == CloudInsightSurface.DAILY_OVERVIEW
    }

    override fun buildUserPrompt(request: CloudInsightRequest): String {
        return buildString {
            appendLine("Generate one concise wellbeing insight for the home dashboard.")
            appendLine("Focus on the most important current behavior pattern.")
            appendLine("Keep the response under 55 words.")
            appendLine("Return plain text only.")
            appendLine()
            append(policy.buildGroundingSection(request))
        }.trim()
    }
}
