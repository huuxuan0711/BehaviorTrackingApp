package com.xmobile.project2digitalwellbeing.domain.reasoning.service

import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightRequest
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightSurface
import javax.inject.Inject

class WeeklyCloudInsightPromptBuilder @Inject constructor(
    private val policy: CloudInsightPromptPolicy
) : CloudInsightPromptBuilder {

    override fun supports(request: CloudInsightRequest): Boolean {
        return request.surface == CloudInsightSurface.WEEKLY_OVERVIEW
    }

    override fun buildUserPrompt(request: CloudInsightRequest): String {
        return buildString {
            appendLine("Generate one concise weekly wellbeing summary for a weekly overview screen.")
            appendLine("Focus on trend direction, momentum across the week, and one useful next step.")
            appendLine("Keep the response under 65 words.")
            appendLine("Return plain text only.")
            appendLine()
            append(policy.buildGroundingSection(request))
        }.trim()
    }
}
