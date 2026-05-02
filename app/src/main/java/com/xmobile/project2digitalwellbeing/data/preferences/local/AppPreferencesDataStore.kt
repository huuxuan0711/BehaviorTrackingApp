package com.xmobile.project2digitalwellbeing.data.preferences.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.xmobile.project2digitalwellbeing.domain.insights.model.InsightSensitivity
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

class AppPreferencesDataStore(private val context: Context) {

    private val dataStore: DataStore<Preferences>
        get() = context.appPreferencesDataStore

    val isIntroCompleted: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[INTRO_COMPLETED_KEY] ?: false
        }

    val usageAnalysisPreferences: Flow<UsageAnalysisPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UsageAnalysisPreferences(
                lateNightStartHour = preferences[LATE_NIGHT_START_HOUR_KEY]
                    ?: UsageAnalysisPreferences.DEFAULT_LATE_NIGHT_START_HOUR,
                longSessionThresholdMillis = (
                    preferences[LONG_SESSION_THRESHOLD_MINUTES_KEY]
                        ?: UsageAnalysisPreferences.DEFAULT_LONG_SESSION_THRESHOLD_MINUTES
                    ) * 60L * 1000L,
                trackAllCategories = preferences[TRACK_ALL_CATEGORIES_KEY]
                    ?: UsageAnalysisPreferences.DEFAULT.trackAllCategories,
                insightSensitivity = preferences[INSIGHT_SENSITIVITY_KEY]
                    ?.toInsightSensitivity()
                    ?: UsageAnalysisPreferences.DEFAULT.insightSensitivity
            )
        }

    suspend fun setIntroCompleted(isCompleted: Boolean) {
        dataStore.edit { preferences ->
            preferences[INTRO_COMPLETED_KEY] = isCompleted
        }
    }

    suspend fun getUsageAnalysisPreferences(): UsageAnalysisPreferences {
        return usageAnalysisPreferences.first()
    }

    suspend fun saveUsageAnalysisPreferences(preferences: UsageAnalysisPreferences) {
        dataStore.edit { editablePreferences ->
            editablePreferences[LATE_NIGHT_START_HOUR_KEY] = preferences.lateNightStartHour
            editablePreferences[LONG_SESSION_THRESHOLD_MINUTES_KEY] =
                (preferences.longSessionThresholdMillis / (60L * 1000L)).toInt()
            editablePreferences[TRACK_ALL_CATEGORIES_KEY] = preferences.trackAllCategories
            editablePreferences[INSIGHT_SENSITIVITY_KEY] = preferences.insightSensitivity.toStorageValue()
        }
    }

    private fun Int.toInsightSensitivity(): InsightSensitivity {
        return when (this) {
            0 -> InsightSensitivity.LOW
            2 -> InsightSensitivity.HIGH
            else -> InsightSensitivity.MEDIUM
        }
    }

    private fun InsightSensitivity.toStorageValue(): Int {
        return when (this) {
            InsightSensitivity.LOW -> 0
            InsightSensitivity.MEDIUM -> 1
            InsightSensitivity.HIGH -> 2
        }
    }

    private companion object {
        const val DATASTORE_NAME = "app_preferences"
        val INTRO_COMPLETED_KEY = booleanPreferencesKey("intro_completed")
        val LATE_NIGHT_START_HOUR_KEY = intPreferencesKey("late_night_start_hour")
        val LONG_SESSION_THRESHOLD_MINUTES_KEY = intPreferencesKey("long_session_threshold_minutes")
        val TRACK_ALL_CATEGORIES_KEY = booleanPreferencesKey("track_all_categories")
        val INSIGHT_SENSITIVITY_KEY = intPreferencesKey("insight_sensitivity")
    }
}

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences"
)
