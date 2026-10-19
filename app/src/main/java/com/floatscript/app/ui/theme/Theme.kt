package com.floatscript.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = BrandBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = AccentAmber,
    background = Ink,
    surface = Surface1,
    surfaceVariant = Surface2,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White,
    onSurfaceVariant = OnSurfaceDim
)

private val LightColors = lightColorScheme(
    primary = BrandBlueDark,
    secondary = AccentAmber
)

/** App-wide Material 3 theme used by every screen and by the overlay window. */
@Composable
fun FloatScriptTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
