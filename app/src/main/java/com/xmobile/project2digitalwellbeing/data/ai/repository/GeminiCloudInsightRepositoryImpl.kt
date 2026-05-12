package com.xmobile.project2digitalwellbeing.data.ai.repository

import android.util.Log
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
import retrofit2.HttpException

class GeminiCloudInsightRepositoryImpl @Inject constructor(
    private val apiService: GeminiApiService,
    private val promptRouter: CloudInsightPromptRouter,
    private val memoryCache: GeminiCloudInsightCache,
    @Named("geminiModelName") private val modelName: String
) : CloudInsightRepository {
    private val logTag = "GeminiInsight"
    private val fallbackModelName = "gemini-2.5-flash"

    override suspend fun generateInsightText(request: CloudInsightRequest): Result<CloudInsightText> {
        return runCatching {
            // 1. Check Cache
            val cachedResult = memoryCache.getCachedInsight(request)
            if (cachedResult != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(
                        logTag,
                        "cache_hit surface=${request.surface} model=${cachedResult.modelName} finish=${cachedResult.finishReason} text=${cachedResult.text.toLogSnippet()}"
                    )
                }
                return@runCatching cachedResult
            }

            // 2. Fetch from API
            val requestBody = GeminiGenerateContentRequest(
                systemInstruction = GeminiContentDto(
                    parts = listOf(GeminiPartDto(promptRouter.buildSystemInstruction(request)))
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
            val selectedModel = modelName.toSupportedGeminiModel()
            var resolvedModelName = selectedModel
            val response = try {
                apiService.generateContent(
                    model = selectedModel,
                    request = requestBody
                )
            } catch (exception: HttpException) {
                if (exception.code() == 404 && selectedModel != fallbackModelName) {
                    if (BuildConfig.DEBUG) {
                        Log.w(
                            logTag,
                            "model_404_retry requested=$modelName normalized=$selectedModel fallback=$fallbackModelName"
                        )
                    }
                    resolvedModelName = fallbackModelName
                    apiService.generateContent(
                        model = fallbackModelName,
                        request = requestBody
                    )
                } else {
                    throw exception
                }
            }
            val candidate = response.candidates?.firstOrNull()
                ?: error("Gemini returned no candidates.")
            val text = candidate.content?.parts
                ?.joinToString(separator = "\n") { it.text }
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: error("Gemini returned an empty response.")

            val result = CloudInsightText(
                text = text,
                modelName = resolvedModelName,
                finishReason = candidate.finishReason
            )
            if (BuildConfig.DEBUG) {
                Log.d(
                    logTag,
                    "llm_returned surface=${request.surface} model=${result.modelName} finish=${result.finishReason} candidates=${response.candidates?.size ?: 0} text=${result.text.toLogSnippet()}"
                )
            }
            
            // 3. Save to Cache
            memoryCache.cacheInsight(request, result)
            
            result
        }
    }

    private fun String.toLogSnippet(maxLen: Int = 200): String {
        val compact = replace("\n", " ").replace("\r", " ").trim()
        return if (compact.length <= maxLen) compact else compact.take(maxLen) + "..."
    }

    private fun String.toSupportedGeminiModel(): String {
        val normalized = trim().removePrefix("models/")
        return when {
            normalized.isBlank() -> fallbackModelName
            normalized == "gemini-3-flash" -> fallbackModelName
            else -> normalized
        }
    }
}
