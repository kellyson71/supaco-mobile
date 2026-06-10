package com.example.supacomobile.data.local

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class BiometricChoice { UNSET, ENABLED, DISABLED }

/** A partir de qual nível de perigo notificar. */
enum class NotifyLevel { WARN, LAST }

class SettingsManager(context: Context) {

    private val prefs = context.getSharedPreferences("supaco_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        ThemeMode.entries.getOrElse(prefs.getInt(KEY_THEME, 0)) { ThemeMode.SYSTEM }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC, true))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _biometricChoice = MutableStateFlow(
        BiometricChoice.entries.getOrElse(prefs.getInt(KEY_BIOMETRIC, 0)) { BiometricChoice.UNSET }
    )
    val biometricChoice: StateFlow<BiometricChoice> = _biometricChoice.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS, true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _notifyLevel = MutableStateFlow(
        NotifyLevel.entries.getOrElse(prefs.getInt(KEY_NOTIFY_LEVEL, 1)) { NotifyLevel.LAST }
    )
    val notifyLevel: StateFlow<NotifyLevel> = _notifyLevel.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putInt(KEY_THEME, mode.ordinal) }
        _themeMode.value = mode
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DYNAMIC, enabled) }
        _dynamicColor.value = enabled
    }

    fun setBiometricChoice(choice: BiometricChoice) {
        prefs.edit { putInt(KEY_BIOMETRIC, choice.ordinal) }
        _biometricChoice.value = choice
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFICATIONS, enabled) }
        _notificationsEnabled.value = enabled
    }

    fun setNotifyLevel(level: NotifyLevel) {
        prefs.edit { putInt(KEY_NOTIFY_LEVEL, level.ordinal) }
        _notifyLevel.value = level
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_DYNAMIC = "dynamic_color"
        const val KEY_BIOMETRIC = "biometric_choice"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_NOTIFY_LEVEL = "notify_level"
    }
}
