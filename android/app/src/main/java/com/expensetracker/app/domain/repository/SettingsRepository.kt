package com.expensetracker.app.domain.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface SettingsRepository {
    suspend fun get(): AppSettings
    suspend fun update(settings: AppSettings)
}

data class AppSettings(
    val baseCurrency: String = "CNY",
    val hideAmount: Boolean = false,
    val appLockEnabled: Boolean = false,
    val language: String = "zh",
    val theme: String = "system",
    val aiBaseUrl: String = "",
    val aiApiKey: String = "",
    val aiModel: String = "gpt-3.5-turbo"
)

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object Keys {
        val BASE_CURRENCY = stringPreferencesKey("base_currency")
        val HIDE_AMOUNT = booleanPreferencesKey("hide_amount")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_MODEL = stringPreferencesKey("ai_model")
    }

    override suspend fun get(): AppSettings = withContext(Dispatchers.IO) {
        val prefs = dataStore.data.first()
        AppSettings(
            baseCurrency = prefs[Keys.BASE_CURRENCY] ?: "CNY",
            hideAmount = prefs[Keys.HIDE_AMOUNT] ?: false,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            language = prefs[Keys.LANGUAGE] ?: "zh",
            theme = prefs[Keys.THEME] ?: "system",
            aiBaseUrl = prefs[Keys.AI_BASE_URL] ?: "",
            aiApiKey = prefs[Keys.AI_API_KEY] ?: "",
            aiModel = prefs[Keys.AI_MODEL] ?: "gpt-3.5-turbo"
        )
    }

    override suspend fun update(settings: AppSettings) = withContext(Dispatchers.IO) {
        dataStore.edit { prefs ->
            prefs[Keys.BASE_CURRENCY] = settings.baseCurrency
            prefs[Keys.HIDE_AMOUNT] = settings.hideAmount
            prefs[Keys.APP_LOCK_ENABLED] = settings.appLockEnabled
            prefs[Keys.LANGUAGE] = settings.language
            prefs[Keys.THEME] = settings.theme
            prefs[Keys.AI_BASE_URL] = settings.aiBaseUrl
            prefs[Keys.AI_API_KEY] = settings.aiApiKey
            prefs[Keys.AI_MODEL] = settings.aiModel
        }
    }
}
