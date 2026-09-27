package io.github.kellyson71.supaco.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class BiometricChoice { UNSET, ENABLED, DISABLED }

/** A partir de qual nível de perigo notificar. */
enum class NotifyLevel { WARN, LAST }

class SettingsManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("supaco_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        ThemeMode.entries.getOrElse(prefs.getInt(KEY_THEME, 0)) { ThemeMode.SYSTEM }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC, true))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    // ── Tema/paleta ──
    private val _paletteId = MutableStateFlow(prefs.getString(KEY_PALETTE, DEFAULT_PALETTE) ?: DEFAULT_PALETTE)
    val paletteId: StateFlow<String> = _paletteId.asStateFlow()

    private val _customSeed = MutableStateFlow(prefs.getInt(KEY_CUSTOM_SEED, DEFAULT_SEED))
    val customSeed: StateFlow<Int> = _customSeed.asStateFlow()

    private val _customStyle = MutableStateFlow(prefs.getInt(KEY_CUSTOM_STYLE, 0))
    val customStyle: StateFlow<Int> = _customStyle.asStateFlow()

    private val _pureBlack = MutableStateFlow(prefs.getBoolean(KEY_PURE_BLACK, false))
    val pureBlack: StateFlow<Boolean> = _pureBlack.asStateFlow()

    // ── Fundo personalizado ──
    private val _backgroundEnabled = MutableStateFlow(prefs.getBoolean(KEY_BG_ENABLED, false))
    val backgroundEnabled: StateFlow<Boolean> = _backgroundEnabled.asStateFlow()

    private val _backgroundOpacity = MutableStateFlow(prefs.getFloat(KEY_BG_OPACITY, 0.45f))
    val backgroundOpacity: StateFlow<Float> = _backgroundOpacity.asStateFlow()

    /** Incrementado a cada troca de imagem para forçar recomposição/recarga do bitmap. */
    private val _backgroundVersion = MutableStateFlow(prefs.getInt(KEY_BG_VERSION, 0))
    val backgroundVersion: StateFlow<Int> = _backgroundVersion.asStateFlow()

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

    fun setPaletteId(id: String) {
        prefs.edit { putString(KEY_PALETTE, id) }
        _paletteId.value = id
    }

    /** Define uma cor-semente personalizada e seleciona a paleta "custom". */
    fun setCustomSeed(argb: Int) {
        prefs.edit {
            putInt(KEY_CUSTOM_SEED, argb)
            putString(KEY_PALETTE, "custom")
        }
        _customSeed.value = argb
        _paletteId.value = "custom"
    }

    fun setCustomStyle(ordinal: Int) {
        prefs.edit {
            putInt(KEY_CUSTOM_STYLE, ordinal)
            putString(KEY_PALETTE, "custom")
        }
        _customStyle.value = ordinal
        _paletteId.value = "custom"
    }

    fun setPureBlack(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_PURE_BLACK, enabled) }
        _pureBlack.value = enabled
    }

    // ── Fundo personalizado ──
    val backgroundFile: File get() = File(context.filesDir, BG_FILENAME)

    fun hasBackgroundFile(): Boolean = backgroundFile.exists()

    /** Copia a imagem escolhida para o armazenamento interno e ativa o fundo. */
    fun saveBackgroundFromUri(uri: Uri): Boolean = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            backgroundFile.outputStream().use { output -> input.copyTo(output) }
        } ?: return false
        prefs.edit {
            putBoolean(KEY_BG_ENABLED, true)
            putInt(KEY_BG_VERSION, _backgroundVersion.value + 1)
        }
        _backgroundVersion.value += 1
        _backgroundEnabled.value = true
        true
    }.getOrDefault(false)

    fun setBackgroundEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BG_ENABLED, enabled) }
        _backgroundEnabled.value = enabled
    }

    fun clearBackground() {
        runCatching { backgroundFile.delete() }
        prefs.edit {
            putBoolean(KEY_BG_ENABLED, false)
            putInt(KEY_BG_VERSION, _backgroundVersion.value + 1)
        }
        _backgroundVersion.value += 1
        _backgroundEnabled.value = false
    }

    fun setBackgroundOpacity(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        prefs.edit { putFloat(KEY_BG_OPACITY, clamped) }
        _backgroundOpacity.value = clamped
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
        const val KEY_PALETTE = "palette_id"
        const val KEY_CUSTOM_SEED = "custom_seed"
        const val KEY_CUSTOM_STYLE = "custom_style"
        const val KEY_PURE_BLACK = "pure_black"
        const val KEY_BG_ENABLED = "bg_enabled"
        const val KEY_BG_OPACITY = "bg_opacity"
        const val KEY_BG_VERSION = "bg_version"
        const val KEY_BIOMETRIC = "biometric_choice"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_NOTIFY_LEVEL = "notify_level"

        const val DEFAULT_PALETTE = "violeta"
        const val DEFAULT_SEED = 0xFF6750A4.toInt()
        const val BG_FILENAME = "custom_background.jpg"
    }
}
