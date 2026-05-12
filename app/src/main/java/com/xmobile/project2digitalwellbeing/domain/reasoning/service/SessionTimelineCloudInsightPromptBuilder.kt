package com.xmobile.project2digitalwellbeing.domain.reasoning.service

import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightRequest
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightSurface
import javax.inject.Inject

class SessionTimelineCloudInsightPromptBuilder @Inject constructor(
    private val policy: CloudInsightPromptPolicy
) : CloudInsightPromptBuilder {

    override fun supports(request: CloudInsightRequest): Boolean {
        return request.surface == CloudInsightSurface.SESSION_TIMELINE
    }

    override fun buildUserPrompt(request: CloudInsightRequest): String {
        return buildString {
            appendLine("Generate one concise session-timeline insight for attention and app-switching behavior.")
            appendLine("Prioritize repeated loops or fragmented flow and include one actionable focus recommendation.")
            appendLine("Keep the response under 60 words.")
            appendLine("Return plain text only.")
            appendLine()
            append(policy.buildGroundingSection(request))
        }.trim()
    }
}
