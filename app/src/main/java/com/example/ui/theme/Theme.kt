package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CyanSeriesA,
    onPrimary = ObsidianBg,
    primaryContainer = Color(0xFF0A3240),
    onPrimaryContainer = CyanSeriesA,
    secondary = AmberSeriesB,
    onSecondary = ObsidianBg,
    secondaryContainer = Color(0xFF3A2A06),
    onSecondaryContainer = AmberSeriesB,
    tertiary = VioletCorrelation,
    onTertiary = ObsidianBg,
    tertiaryContainer = Color(0xFF281F47),
    onTertiaryContainer = VioletCorrelation,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = CarbonSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ElevatedSurface,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderHairline,
    outlineVariant = SurfaceVariantDark,
    error = CoralBear,
    onError = Color.White,
    errorContainer = Color(0xFF3F121D),
    onErrorContainer = CoralBear
)

private val LightColorScheme = lightColorScheme(
    primary = CyanSeriesALight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0F4F8),
    onPrimaryContainer = Color(0xFF00363E),
    secondary = AmberSeriesBLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = VioletCorrelationLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF2E1065),
    background = CrispBgLight,
    onBackground = TextPrimaryLight,
    surface = CrispSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = CrispElevatedLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFE11D48),
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF881337)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun CorrelaPulseTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
