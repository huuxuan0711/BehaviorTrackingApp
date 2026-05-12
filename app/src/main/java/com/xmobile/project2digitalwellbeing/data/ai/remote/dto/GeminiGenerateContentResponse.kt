package com.xmobile.project2digitalwellbeing.data.ai.remote.dto

data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidateDto>? = null
)

data class GeminiCandidateDto(
    val content: GeminiContentDto? = null,
    val finishReason: String? = null
)
