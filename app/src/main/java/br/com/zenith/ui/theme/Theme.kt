package br.com.zenith.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

private val DarkColorScheme = darkColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1B6F1D),
    onPrimaryContainer = Color(0xFFEAF3DE),
    inversePrimary = SecondaryGreen,
    secondary = SecondaryGreen,
    onSecondary = Color(0xFF143B15),
    secondaryContainer = Color(0xFF2B792C),
    onSecondaryContainer = Color(0xFFEAF3DE),
    tertiary = Color(0xFFF26500),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF7A3500),
    onTertiaryContainer = Color(0xFFFFE1C2),
    background = Color(0xFF101510),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF101510),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF283028),
    onSurfaceVariant = Color(0xFFE0E0E0),
    surfaceTint = Green,
    inverseSurface = Color(0xFFEAF3DE),
    inverseOnSurface = Color(0xFF101510),
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFF6F1D1D),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8A948A),
    outlineVariant = Color(0xFF455045),
    scrim = Color.Black
)
private val LightColorScheme = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAF3DE),
    onPrimaryContainer = Color(0xFF094000),
    inversePrimary = SecondaryGreen,
    secondary = SecondaryGreen,
    onSecondary = Color(0xFF143B15),
    secondaryContainer = Color(0xFFE9F8E9),
    onSecondaryContainer = Color(0xFF143B15),
    tertiary = Color(0xFFF26500),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE9D6),
    onTertiaryContainer = Color(0xFF5A2500),
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF1F4F1),
    onSurfaceVariant = Color(0xFF4E5D4F),
    surfaceTint = Green,
    inverseSurface = Color(0xFF1A1A1A),
    inverseOnSurface = Color.White,
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFEEEE),
    onErrorContainer = Color(0xFF8A1F1F),
    outline = Color(0xFF8A948A),
    outlineVariant = Color(0xFFE1EAE1),
    scrim = Color.Black
)

@Composable
fun ZenithTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        ProvideTextStyle(value = TextStyle(fontFamily = Inter)) {
            content()
        }
    }
}
