package com.zephyrus.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Custom color palette
private val Purple80 = Color(0xFFD0BCFF)
private val PurpleGrey80 = Color(0xFFCCC2DC)
private val Pink80 = Color(0xFFEFB8C8)

private val Purple40 = Color(0xFF6650a4)
private val PurpleGrey40 = Color(0xFF625b71)
private val Pink40 = Color(0xFF7D5260)

// Brand colors
val ZephyrusBlue = Color(0xFF2196F3)
val ZephyrusDark = Color(0xFF1565C0)
val Success = Color(0xFF4CAF50)
val Warning = Color(0xFFFF9800)
val Danger = Color(0xFFF44336)

private val DarkColorScheme = darkColorScheme(
    primary = ZephyrusBlue,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    surface = Color(0xFF121212),
    background = Color(0xFF0A0A0A),
)

private val LightColorScheme = lightColorScheme(
    primary = ZephyrusDark,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    surface = Color(0xFFFFFBFE),
    background = Color(0xFFFFFBFE),
)

@Composable
fun ZephyrusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
