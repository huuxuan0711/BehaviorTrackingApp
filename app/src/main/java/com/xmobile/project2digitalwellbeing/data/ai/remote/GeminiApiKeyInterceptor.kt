package com.xmobile.project2digitalwellbeing.data.ai.remote

import com.xmobile.project2digitalwellbeing.domain.reasoning.repository.CloudSecretRepository
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class GeminiApiKeyInterceptor @Inject constructor(
    private val cloudSecretRepository: CloudSecretRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = cloudSecretRepository.getGeminiApiKey()
        check(apiKey.isNotBlank()) {
            "Gemini API key is missing. Save your Google AI Studio API key before enabling cloud enhancement."
        }

        val request = chain.request()
            .newBuilder()
            .header("x-goog-api-key", apiKey)
            .header("Content-Type", "application/json")
            .build()
        return chain.proceed(request)
    }
}
