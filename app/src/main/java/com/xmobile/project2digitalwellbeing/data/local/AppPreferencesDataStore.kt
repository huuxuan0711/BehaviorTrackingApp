package com.xmobile.project2digitalwellbeing.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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

    suspend fun setIntroCompleted(isCompleted: Boolean) {
        dataStore.edit { preferences ->
            preferences[INTRO_COMPLETED_KEY] = isCompleted
        }
    }

    private companion object {
        const val DATASTORE_NAME = "app_preferences"
        val INTRO_COMPLETED_KEY = booleanPreferencesKey("intro_completed")
    }
}

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences"
)
