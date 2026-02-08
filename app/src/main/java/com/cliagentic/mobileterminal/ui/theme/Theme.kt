package com.cliagentic.mobileterminal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkScheme = darkColorScheme(
    primary = RedPrimaryLight,
    onPrimary = Color(0xFF690005),
    primaryContainer = RedPrimaryContainer,
    onPrimaryContainer = OnRedPrimaryContainer,
    secondary = AmberSecondary,
    onSecondary = Color(0xFF3E2E00),
    secondaryContainer = AmberSecondaryContainer,
    onSecondaryContainer = OnAmberSecondaryContainer,
    tertiary = SlateTertiary,
    onTertiary = Color(0xFF003544),
    tertiaryContainer = SlateContainer,
    onTertiaryContainer = Color(0xFFB8E0ED),
    error = ErrorRed,
    onError = Color(0xFF601410),
    errorContainer = ErrorContainer,
    onErrorContainer = Color(0xFFFFDAD6),
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF1C1B1F),
    inversePrimary = RedPrimaryDark,
    surfaceDim = SurfaceDim,
    surfaceBright = SurfaceBright,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    scrim = Color(0xFF000000)
)

private val LightScheme = lightColorScheme(
    primary = RedPrimary,
    onPrimary = Color.White,
    primaryContainer = RedPrimaryContainerLight,
    onPrimaryContainer = OnRedPrimaryContainerLight,
    secondary = AmberSecondaryDark,
    onSecondary = Color.White,
    secondaryContainer = AmberSecondaryContainerLight,
    onSecondaryContainer = OnAmberSecondaryContainerLight,
    tertiary = Color(0xFF4A6267),
    onTertiary = Color.White,
    tertiaryContainer = SlateContainerLight,
    onTertiaryContainer = Color(0xFF051F24),
    error = ErrorRedLight,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = Color(0xFF410E0B),
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceContainerHighLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFFFB4AB),
    surfaceDim = SurfaceDimLight,
    surfaceBright = SurfaceBrightLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    scrim = Color(0xFF000000)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun TerminalPilotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
