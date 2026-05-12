package com.xmobile.project2digitalwellbeing.domain.reasoning.service

import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightRequest

interface CloudInsightPromptBuilder {
    fun supports(request: CloudInsightRequest): Boolean

    fun buildUserPrompt(request: CloudInsightRequest): String
}
