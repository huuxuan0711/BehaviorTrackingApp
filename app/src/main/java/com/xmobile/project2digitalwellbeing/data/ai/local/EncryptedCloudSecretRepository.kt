package com.xmobile.project2digitalwellbeing.data.ai.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.xmobile.project2digitalwellbeing.BuildConfig
import com.xmobile.project2digitalwellbeing.domain.reasoning.repository.CloudSecretRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class EncryptedCloudSecretRepository @Inject constructor(
    @ApplicationContext context: Context
) : CloudSecretRepository {

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFERENCES_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun getGeminiApiKey(): String {
        val apiKey = sharedPreferences.getString(PREF_KEY_GEMINI_API_KEY, "").orEmpty().trim()
        if (apiKey.isNotEmpty()) {
            return apiKey
        }
        return BuildConfig.GEMINI_API_KEY
    }

    override fun hasGeminiApiKey(): Boolean {
        return getGeminiApiKey().isNotBlank()
    }

    override fun saveGeminiApiKey(apiKey: String) {
        sharedPreferences.edit()
            .putString(PREF_KEY_GEMINI_API_KEY, apiKey.trim())
            .apply()
    }

    private companion object {
        private const val PREFERENCES_NAME = "cloud_secrets"
        private const val PREF_KEY_GEMINI_API_KEY = "gemini_api_key"
    }
}
