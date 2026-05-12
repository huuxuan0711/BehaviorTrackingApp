package com.xmobile.project2digitalwellbeing.data.ai.remote.dto

data class GeminiGenerateContentRequest(
    val systemInstruction: GeminiContentDto? = null,
    val contents: List<GeminiContentDto>
)

data class GeminiContentDto(
    val role: String? = null,
    val parts: List<GeminiPartDto>
)

data class GeminiPartDto(
    val text: String
)
