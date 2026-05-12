package com.xmobile.project2digitalwellbeing.data.ai.repository

import com.xmobile.project2digitalwellbeing.BuildConfig
import com.xmobile.project2digitalwellbeing.data.ai.remote.api.GeminiApiService
import com.xmobile.project2digitalwellbeing.data.ai.remote.dto.GeminiContentDto
import com.xmobile.project2digitalwellbeing.data.ai.remote.dto.GeminiGenerateContentRequest
import com.xmobile.project2digitalwellbeing.data.ai.remote.dto.GeminiPartDto
import com.xmobile.project2digitalwellbeing.data.ai.local.GeminiCloudInsightCache
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightRequest
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightText
import com.xmobile.project2digitalwellbeing.domain.reasoning.repository.CloudInsightRepository
import com.xmobile.project2digitalwellbeing.domain.reasoning.service.CloudInsightPromptRouter
import javax.inject.Inject
import javax.inject.Named

class GeminiCloudInsightRepositoryImpl @Inject constructor(
    private val apiService: GeminiApiService,
    private val promptRouter: CloudInsightPromptRouter,
    private val memoryCache: GeminiCloudInsightCache,
    @Named("geminiModelName") private val modelName: String
) : CloudInsightRepository {

    override suspend fun generateInsightText(request: CloudInsightRequest): Result<CloudInsightText> {
        return runCatching {
            // 1. Check Cache
            val cachedResult = memoryCache.getCachedInsight(request)
            if (cachedResult != null) {
                return@runCatching cachedResult
            }

            // 2. Fetch from API
            val response = apiService.generateContent(
                model = modelName,
                request = GeminiGenerateContentRequest(
                    systemInstruction = GeminiContentDto(
                        parts = listOf(GeminiPartDto(promptRouter.buildSystemInstruction()))
                    ),
                    contents = listOf(
                        GeminiContentDto(
                            role = "user",
                            parts = listOf(
                                GeminiPartDto(promptRouter.buildUserPrompt(request))
                            )
                        )
                    )
                )
            )
            val candidate = response.candidates?.firstOrNull()
                ?: error("Gemini returned no candidates.")
            val text = candidate.content?.parts
                ?.joinToString(separator = "\n") { it.text }
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: error("Gemini returned an empty response.")

            val result = CloudInsightText(
                text = text,
                modelName = modelName.ifBlank { BuildConfig.GEMINI_MODEL_NAME },
                finishReason = candidate.finishReason
            )
            
            // 3. Save to Cache
            memoryCache.cacheInsight(request, result)
            
            result
        }
    }
}
