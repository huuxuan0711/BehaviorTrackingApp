package com.xmobile.project2digitalwellbeing.data.ai.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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
        return sharedPreferences.getString(GEMINI_API_KEY, "").orEmpty().trim()
    }

    override fun hasGeminiApiKey(): Boolean {
        return getGeminiApiKey().isNotBlank()
    }

    override fun saveGeminiApiKey(apiKey: String) {
        sharedPreferences.edit()
            .putString(GEMINI_API_KEY, apiKey.trim())
            .apply()
    }

    private companion object {
        private const val PREFERENCES_NAME = "cloud_secrets"
        private const val GEMINI_API_KEY = "AIzaSyA5e0rqHQactAkGK4qli5oud3GmlPsD8IQ"
    }
}
