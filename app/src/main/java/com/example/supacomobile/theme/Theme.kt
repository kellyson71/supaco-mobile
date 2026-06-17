package com.example.supacomobile.theme

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

@Immutable
data class VerdictColors(
    val goContainer: Color,
    val onGoContainer: Color,
    val goSolid: Color,
    val onGoSolid: Color,
    val warnContainer: Color,
    val onWarnContainer: Color,
    val warnSolid: Color,
    val onWarnSolid: Color,
    val lastContainer: Color,
    val onLastContainer: Color,
    val lastSolid: Color,
    val onLastSolid: Color,
    val noContainer: Color,
    val onNoContainer: Color,
    val noSolid: Color,
    val onNoSolid: Color,
)

private val LightVerdictColors = VerdictColors(
    goContainer = GoContainer, onGoContainer = OnGoContainer, goSolid = GoSolid, onGoSolid = Color.White,
    warnContainer = WarnContainer, onWarnContainer = OnWarnContainer, warnSolid = WarnSolid, onWarnSolid = Color.White,
    lastContainer = LastContainer, onLastContainer = OnLastContainer, lastSolid = LastSolid, onLastSolid = Color.White,
    noContainer = NoContainer, onNoContainer = OnNoContainer, noSolid = NoSolid, onNoSolid = Color.White,
)

private val DarkVerdictColors = VerdictColors(
    goContainer = GoContainerDark, onGoContainer = OnGoContainerDark, goSolid = GoSolidDark, onGoSolid = Color(0xFF00371E),
    warnContainer = WarnContainerDark, onWarnContainer = OnWarnContainerDark, warnSolid = WarnSolidDark, onWarnSolid = Color(0xFF271A00),
    lastContainer = LastContainerDark, onLastContainer = OnLastContainerDark, lastSolid = LastSolidDark, onLastSolid = Color(0xFF2D1000),
    noContainer = NoContainerDark, onNoContainer = OnNoContainerDark, noSolid = NoSolidDark, onNoSolid = Color(0xFF410002),
)

val LocalVerdictColors = staticCompositionLocalOf { LightVerdictColors }

val MaterialTheme.verdictColors: VerdictColors
    @Composable get() = LocalVerdictColors.current

/** Imagem de fundo personalizada já decodificada + intensidade (0 = invisível, 1 = sem véu). */
@Immutable
data class AppBackground(
    val bitmap: ImageBitmap,
    val opacity: Float,
)

@Composable
fun SupacoMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    seedColor: Color = Color(0xFF6750A4),
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    amoled: Boolean = false,
    background: AppBackground? = null,
    content: @Composable () -> Unit,
) {
    val useAmoled = amoled && darkTheme
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val scheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (useAmoled) scheme.copy(background = Color.Black, surface = Color.Black) else scheme
        }
        else -> rememberDynamicColorScheme(
            seedColor = seedColor,
            isDark = darkTheme,
            isAmoled = useAmoled,
            style = paletteStyle,
        )
    }

    // Quando há fundo personalizado, deixamos o background do tema transparente
    // para a imagem aparecer atrás dos Scaffolds (os cards continuam opacos/legíveis).
    val colorScheme = if (background != null) baseScheme.copy(background = Color.Transparent) else baseScheme

    val verdictColors = if (darkTheme) DarkVerdictColors else LightVerdictColors

    CompositionLocalProvider(LocalVerdictColors provides verdictColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
        ) {
            if (background != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        bitmap = background.bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Véu sobre a foto, com a cor de fundo original do tema, para
                    // manter legibilidade. Quanto maior a opacidade escolhida, mais a foto aparece.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(baseScheme.background.copy(alpha = 1f - background.opacity)),
                    )
                    content()
                }
            } else {
                content()
            }
        }
    }
}
