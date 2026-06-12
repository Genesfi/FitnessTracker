package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ProfessionalPolishColorScheme = lightColorScheme(
    primary = GymPrimary,
    secondary = GymSecondary,
    tertiary = GymTertiary,
    background = ProfessionalBackground,
    surface = ProfessionalSurface,
    onPrimary = ProfessionalSurface, // White text on dark primary
    onSecondary = ProfessionalSurface, // White text on dark secondary
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = ProfessionalSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    outlineVariant = ProfessionalOutlineVariant
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // We enforce our custom light professional polish styling to guarantee a cohesive, high-fidelity experience
    MaterialTheme(
        colorScheme = ProfessionalPolishColorScheme,
        typography = Typography,
        content = content
    )
}
