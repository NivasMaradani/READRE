package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ReaderDarkColorScheme = darkColorScheme(
    primary = PrimaryText,
    onPrimary = MainBackground,
    primaryContainer = CardBackground,
    onPrimaryContainer = PrimaryText,
    secondary = AccentColor,
    onSecondary = MainBackground,
    background = MainBackground,
    onBackground = PrimaryText,
    surface = SecondaryBackground,
    onSurface = PrimaryText,
    surfaceVariant = CardBackground,
    onSurfaceVariant = SecondaryText,
    outline = DividerColor,
    error = HighlightRed,
    onError = PrimaryText
)

@Composable
fun ReaderTheme(
    darkTheme: Boolean = true, // Reader is dark-theme focused for minimal distraction
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ReaderDarkColorScheme,
        typography = Typography,
        content = content
    )
}
