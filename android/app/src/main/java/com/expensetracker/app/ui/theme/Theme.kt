package com.expensetracker.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = SurfaceLight,
    primaryContainer = AccentLight,
    onPrimaryContainer = AccentDark,
    secondary = TextSecondary,
    onSecondary = SurfaceLight,
    secondaryContainer = BackgroundLight,
    onSecondaryContainer = TextPrimary,
    tertiary = Warning,
    error = Error,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundLight,
    onSurfaceVariant = TextSecondary,
    outline = Border,
    outlineVariant = Divider
)

@Composable
fun ExpenseTrackerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
