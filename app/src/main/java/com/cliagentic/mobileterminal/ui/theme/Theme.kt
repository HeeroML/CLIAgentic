package com.cliagentic.mobileterminal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = NeonMint,
    secondary = Ember,
    background = Night,
    surface = InkBlue,
    onPrimary = Night,
    onSecondary = Night,
    onBackground = Cloud,
    onSurface = Cloud
)

private val LightScheme = lightColorScheme(
    primary = SteelBlue,
    secondary = Ember,
    background = ColorTokens.LightBackground,
    surface = ColorTokens.LightSurface,
    onPrimary = Cloud,
    onSecondary = Night,
    onBackground = Night,
    onSurface = Night
)

private object ColorTokens {
    val LightBackground = Cloud
    val LightSurface = Color(0xFFF7FBFF)
}

@Composable
fun TerminalPilotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        content = content
    )
}
