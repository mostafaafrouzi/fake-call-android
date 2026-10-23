package com.afrouzi.fakecall.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fake_call_settings")

class SettingsRepository(private val context: Context) {

    private val KEY_THEME = stringPreferencesKey("theme_mode")
    private val KEY_LANGUAGE = stringPreferencesKey("language")
    private val KEY_FONT = stringPreferencesKey("font")
    private val KEY_DIGITS = stringPreferencesKey("digits")
    private val KEY_VIEW_MODE = stringPreferencesKey("view_mode")
    private val KEY_SHOW_SYSTEM = booleanPreferencesKey("show_system_apps")
    private val KEY_CALL_THEME = stringPreferencesKey("call_theme")
    private val KEY_ASK_SAVE_ADHOC = booleanPreferencesKey("ask_save_adhoc")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[KEY_THEME] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            language = runCatching {
                AppLanguage.valueOf(prefs[KEY_LANGUAGE] ?: AppLanguage.SYSTEM.name)
            }.getOrDefault(AppLanguage.SYSTEM),
            font = runCatching {
                AppFont.valueOf(prefs[KEY_FONT] ?: AppFont.IRAN_SANS.name)
            }.getOrDefault(AppFont.IRAN_SANS),
            digitStyle = runCatching {
                DigitStyle.valueOf(prefs[KEY_DIGITS] ?: DigitStyle.PERSIAN.name)
            }.getOrDefault(DigitStyle.PERSIAN),
            viewMode = runCatching {
                ViewMode.valueOf(prefs[KEY_VIEW_MODE] ?: ViewMode.CASUAL.name)
            }.getOrDefault(ViewMode.CASUAL),
            showSystemApps = prefs[KEY_SHOW_SYSTEM] ?: false,
            callTheme = runCatching {
                CallTheme.valueOf(prefs[KEY_CALL_THEME] ?: CallTheme.ONE_UI.name)
            }.getOrDefault(CallTheme.ONE_UI),
            askSaveProfileOnAdhoc = prefs[KEY_ASK_SAVE_ADHOC] ?: true,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language.name }
    }

    suspend fun setFont(font: AppFont) {
        context.dataStore.edit { it[KEY_FONT] = font.name }
    }

    suspend fun setDigitStyle(style: DigitStyle) {
        context.dataStore.edit { it[KEY_DIGITS] = style.name }
    }

    suspend fun setViewMode(mode: ViewMode) {
        context.dataStore.edit { it[KEY_VIEW_MODE] = mode.name }
    }

    suspend fun setShowSystemApps(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_SYSTEM] = show }
    }

    suspend fun setCallTheme(theme: CallTheme) {
        context.dataStore.edit { it[KEY_CALL_THEME] = theme.name }
    }

    suspend fun setAskSaveProfileOnAdhoc(ask: Boolean) {
        context.dataStore.edit { it[KEY_ASK_SAVE_ADHOC] = ask }
    }
}
