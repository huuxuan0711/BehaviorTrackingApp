package com.xmobile.project2digitalwellbeing.domain.reasoning.repository

interface CloudSecretRepository {
    fun getGeminiApiKey(): String

    fun hasGeminiApiKey(): Boolean

    fun saveGeminiApiKey(apiKey: String)
}
