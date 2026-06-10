package com.example.supacomobile.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
)

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

@Composable
fun SupacoMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val verdictColors = if (darkTheme) DarkVerdictColors else LightVerdictColors

    CompositionLocalProvider(LocalVerdictColors provides verdictColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
