package jp.hotdrop.simpledyphic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B8FC7),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E8FF),
    onPrimaryContainer = Color(0xFF102B46),
    secondary = Color(0xFF6A839F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E4FF),
    onSecondaryContainer = Color(0xFF233548),
    tertiary = Color(0xFF6885A8),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD4E4FF),
    onTertiaryContainer = Color(0xFF21324A),
    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF181C22),
    surface = Color(0xFFF7F9FF),
    onSurface = Color(0xFF181C22),
    surfaceVariant = Color(0xFFDEE4EE),
    onSurfaceVariant = Color(0xFF414A56),
    outline = Color(0xFF717985)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAFCBEE),
    onPrimary = Color(0xFF183250),
    primaryContainer = Color(0xFF324D6B),
    onPrimaryContainer = Color(0xFFD8E8FF),
    secondary = Color(0xFFB4C7E3),
    onSecondary = Color(0xFF1D3349),
    secondaryContainer = Color(0xFF334A61),
    onSecondaryContainer = Color(0xFFD1E4FF),
    tertiary = Color(0xFFB5C8E9),
    onTertiary = Color(0xFF1A334F),
    tertiaryContainer = Color(0xFF334A66),
    onTertiaryContainer = Color(0xFFD4E4FF),
    background = Color(0xFF101419),
    onBackground = Color(0xFFE2E7EF),
    surface = Color(0xFF101419),
    onSurface = Color(0xFFE2E7EF),
    surfaceVariant = Color(0xFF414A56),
    onSurfaceVariant = Color(0xFFC0C7D2),
    outline = Color(0xFF8A929D)
)

@Composable
fun SimpleDyphicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
