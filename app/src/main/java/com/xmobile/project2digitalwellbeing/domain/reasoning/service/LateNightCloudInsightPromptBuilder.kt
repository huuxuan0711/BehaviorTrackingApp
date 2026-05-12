package com.xmobile.project2digitalwellbeing.domain.reasoning.service

import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightRequest
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightSurface
import javax.inject.Inject

class LateNightCloudInsightPromptBuilder @Inject constructor(
    private val policy: CloudInsightPromptPolicy
) : CloudInsightPromptBuilder {

    override fun supports(request: CloudInsightRequest): Boolean {
        return request.surface == CloudInsightSurface.LATE_NIGHT_ANALYSIS
    }

    override fun buildUserPrompt(request: CloudInsightRequest): String {
        return buildString {
            appendLine("Generate one concise late-night behavior insight for a sleep-focused analysis screen.")
            appendLine("Emphasize timing, intensity, and one realistic night routine adjustment.")
            appendLine("Keep the response under 60 words.")
            appendLine("Return plain text only.")
            appendLine()
            append(policy.buildGroundingSection(request))
        }.trim()
    }
}
