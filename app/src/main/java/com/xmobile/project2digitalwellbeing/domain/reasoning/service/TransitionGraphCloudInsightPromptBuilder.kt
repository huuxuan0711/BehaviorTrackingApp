package com.xmobile.project2digitalwellbeing.domain.reasoning.service

import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightRequest
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightSurface
import javax.inject.Inject

class TransitionGraphCloudInsightPromptBuilder @Inject constructor(
    private val policy: CloudInsightPromptPolicy
) : CloudInsightPromptBuilder {

    override fun supports(request: CloudInsightRequest): Boolean {
        return request.surface == CloudInsightSurface.TRANSITION_GRAPH
    }

    override fun buildUserPrompt(request: CloudInsightRequest): String {
        return buildString {
            appendLine("Generate one concise transition behavior insight for a graph visualization screen.")
            appendLine("Explain the primary transition patterns (e.g. loops between A and B, or deep rabbit holes).")
            appendLine("Highlight loop dominant apps or chains if any.")
            appendLine("Keep the response structured and under 80 words.")
            appendLine("Return plain text only.")
            appendLine()
            append(policy.buildGroundingSection(request))
        }.trim()
    }
}
