package com.xmobile.project2digitalwellbeing.data.ai.remote.api

import com.xmobile.project2digitalwellbeing.data.ai.remote.dto.GeminiGenerateContentRequest
import com.xmobile.project2digitalwellbeing.data.ai.remote.dto.GeminiGenerateContentResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Body request: GeminiGenerateContentRequest
    ): GeminiGenerateContentResponse
}
