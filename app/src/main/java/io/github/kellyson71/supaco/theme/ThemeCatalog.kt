package io.github.kellyson71.supaco.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle

/**
 * Estilos de geração de paleta (Material Color Utilities, via material-kolor).
 * Mudam como as cores derivam da cor-semente: mais suaves, mais vibrantes,
 * ou totalmente monocromáticas (perfeito para OLED).
 */
enum class AppPaletteStyle(val label: String, val style: PaletteStyle) {
    SUAVE("Suave", PaletteStyle.TonalSpot),
    NEUTRO("Neutro", PaletteStyle.Neutral),
    VIBRANTE("Vibrante", PaletteStyle.Vibrant),
    EXPRESSIVO("Expressivo", PaletteStyle.Expressive),
    FIEL("Fiel", PaletteStyle.Fidelity),
    MONO("Mono", PaletteStyle.Monochrome);

    companion object {
        fun fromOrdinalOrDefault(ordinal: Int): AppPaletteStyle =
            entries.getOrElse(ordinal) { SUAVE }
    }
}

/**
 * Tema pré-definido: uma cor-semente + estilo. A paleta completa (light/dark,
 * containers, etc.) é gerada em tempo de execução a partir disso.
 */
data class ThemePreset(
    val id: String,
    val label: String,
    val seed: Color,
    val style: AppPaletteStyle,
    /** Força fundo preto puro no modo escuro — ideal para telas OLED. */
    val amoled: Boolean = false,
    /** Cor exibida no seletor (amostra). Por padrão é a própria semente. */
    val swatch: Color = seed,
)

const val CUSTOM_PALETTE_ID = "custom"

/** Temas prontos. O último (OLED Mono) é monocromático + preto puro. */
val ThemePresets: List<ThemePreset> = listOf(
    ThemePreset("violeta", "Violeta", Color(0xFF6750A4), AppPaletteStyle.SUAVE),
    ThemePreset("oceano", "Oceano", Color(0xFF1E6FD9), AppPaletteStyle.SUAVE),
    ThemePreset("floresta", "Floresta", Color(0xFF2E7D52), AppPaletteStyle.SUAVE),
    ThemePreset("por_do_sol", "Pôr do sol", Color(0xFFE2682B), AppPaletteStyle.VIBRANTE),
    ThemePreset("rosa", "Rosa", Color(0xFFD6457E), AppPaletteStyle.EXPRESSIVO),
    ThemePreset("carmesim", "Carmesim", Color(0xFFC1322F), AppPaletteStyle.VIBRANTE),
    ThemePreset("oled", "OLED Mono", Color(0xFFE7E7EA), AppPaletteStyle.MONO, amoled = true, swatch = Color(0xFF0A0A0A)),
)

fun presetById(id: String?): ThemePreset? = ThemePresets.firstOrNull { it.id == id }

/** Cor-semente, estilo e amoled efetivamente resolvidos a partir das preferências. */
data class ResolvedPalette(
    val seed: Color,
    val style: PaletteStyle,
    val amoled: Boolean,
)

/**
 * Resolve a paleta a partir do que está salvo nas preferências.
 * Para temas prontos usa o catálogo; para "custom" usa a cor/estilo do usuário.
 * O toggle de preto puro do usuário sempre pode adicionar AMOLED por cima.
 */
fun resolvePalette(
    paletteId: String,
    customSeedArgb: Int,
    customStyleOrdinal: Int,
    pureBlack: Boolean,
): ResolvedPalette {
    val preset = presetById(paletteId)
    return if (preset != null) {
        ResolvedPalette(
            seed = preset.seed,
            style = preset.style.style,
            amoled = preset.amoled || pureBlack,
        )
    } else {
        ResolvedPalette(
            seed = Color(customSeedArgb),
            style = AppPaletteStyle.fromOrdinalOrDefault(customStyleOrdinal).style,
            amoled = pureBlack,
        )
    }
}
