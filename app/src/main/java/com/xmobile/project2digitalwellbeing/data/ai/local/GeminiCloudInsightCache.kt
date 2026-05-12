package com.xmobile.project2digitalwellbeing.data.ai.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightRequest
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightText
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.cloudInsightCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "cloud_insight_cache")

@Singleton
class GeminiCloudInsightCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val dataStore = context.cloudInsightCacheDataStore

    companion object {
        private const val CACHE_EXPIRATION_MILLIS = 3600_000L // 1 hour
    }

    suspend fun getCachedInsight(request: CloudInsightRequest): CloudInsightText? {
        val hash = generateHash(request)
        val textKey = stringPreferencesKey("insight_text_$hash")
        val timeKey = longPreferencesKey("insight_time_$hash")

        val preferences = dataStore.data.first()
        val cacheTime = preferences[timeKey] ?: 0L

        if (System.currentTimeMillis() - cacheTime > CACHE_EXPIRATION_MILLIS) {
            return null
        }

        val cachedJson = preferences[textKey]
        return if (cachedJson != null) {
            try {
                gson.fromJson(cachedJson, CloudInsightText::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    suspend fun cacheInsight(request: CloudInsightRequest, insightText: CloudInsightText) {
        val hash = generateHash(request)
        val textKey = stringPreferencesKey("insight_text_$hash")
        val timeKey = longPreferencesKey("insight_time_$hash")

        dataStore.edit { preferences ->
            preferences[textKey] = gson.toJson(insightText)
            preferences[timeKey] = System.currentTimeMillis()
        }
    }

    private fun generateHash(request: CloudInsightRequest): String {
        // We hash surface and groundedContext. fallbackInsight doesn't change the AI prompt significantly.
        val dataToHash = request.surface.name + "|" + gson.toJson(request.groundedContext)
        val bytes = MessageDigest.getInstance("MD5").digest(dataToHash.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
