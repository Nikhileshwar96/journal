package com.nikhil.journal.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF3E5C76),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0E4FF),
    onPrimaryContainer = Color(0xFF001D35),
    secondary = Color(0xFF8A5A00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB3),
    onSecondaryContainer = Color(0xFF2B1700),
    background = Color(0xFFFFF8F1),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFFF8F1),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFEDE1D4),
    onSurfaceVariant = Color(0xFF4F4539),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA6C8E8),
    onPrimary = Color(0xFF0B3350),
    primaryContainer = Color(0xFF254A67),
    onPrimaryContainer = Color(0xFFD0E4FF),
    secondary = Color(0xFFFFB950),
    onSecondary = Color(0xFF482900),
    secondaryContainer = Color(0xFF673D00),
    onSecondaryContainer = Color(0xFFFFDDB3),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7CF),
)

/** Material 3 theme; follows the system light/dark setting and uses dynamic color on Android 12+. */
@Composable
fun JournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
