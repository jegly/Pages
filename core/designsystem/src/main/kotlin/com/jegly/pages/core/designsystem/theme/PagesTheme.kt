package com.jegly.pages.core.designsystem.theme

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

private val PagesDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB59A),
    onPrimary = Color(0xFF5E1A00),
    primaryContainer = Color(0xFF862400),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFFE7BDB1),
    onSecondary = Color(0xFF44291C),
    secondaryContainer = Color(0xFF5D3F37),
    onSecondaryContainer = Color(0xFFFFDBCF),
    tertiary = Color(0xFFD5C48A),
    onTertiary = Color(0xFF3A2F00),
    tertiaryContainer = Color(0xFF544500),
    onTertiaryContainer = Color(0xFFF2E0A4),
    background = Color(0xFF201A18),
    surface = Color(0xFF201A18),
    surfaceVariant = Color(0xFF53433F),
    onBackground = Color(0xFFEDE0DC),
    onSurface = Color(0xFFEDE0DC),
    onSurfaceVariant = Color(0xFFD8C2BC),
    outline = Color(0xFFA08C87),
    outlineVariant = Color(0xFF53433F),
)

private val PagesLightColorScheme = lightColorScheme(
    primary = Color(0xFF9C3714),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF3A0800),
    secondary = Color(0xFF77574E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBD1),
    onSecondaryContainer = Color(0xFF2C150E),
    tertiary = Color(0xFF6D5D2C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6E0A4),
    onTertiaryContainer = Color(0xFF251A00),
    background = Color(0xFFFFF8F6),
    surface = Color(0xFFFFF8F6),
    surfaceVariant = Color(0xFFEDD2CC),
    onBackground = Color(0xFF201A18),
    onSurface = Color(0xFF201A18),
    onSurfaceVariant = Color(0xFF53433F),
    outline = Color(0xFF867068),
    outlineVariant = Color(0xFFEDD2CC),
)

private val EInkLightColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E0E0),
    onPrimaryContainer = Color(0xFF000000),
    secondary = Color(0xFF444444),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDDDDD),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFF222222),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCCCCC),
    onTertiaryContainer = Color(0xFF000000),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEEEEE),
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000),
    onSurfaceVariant = Color(0xFF444444),
    outline = Color(0xFF888888),
    outlineVariant = Color(0xFFCCCCCC),
)

private val EInkDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF222222),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFCCCCCC),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF333333),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFBBBBBB),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF444444),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF111111),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFCCCCCC),
    outline = Color(0xFF888888),
    outlineVariant = Color(0xFF333333),
)

/**
 * @param darkTheme     Whether to use dark variant.
 * @param dynamicColor  Use wallpaper-derived colours on Android 12+ (S).
 * @param eInkMode      Override with a greyscale/no-animation scheme.
 * @param seedColor     If non-null, replaces the default warm-rust primary with this colour.
 */
@Composable
fun PagesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    eInkMode: Boolean = false,
    seedColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        eInkMode -> if (darkTheme) EInkDarkColorScheme else EInkLightColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        seedColor != null -> {
            if (darkTheme) PagesDarkColorScheme.copy(primary = seedColor)
            else PagesLightColorScheme.copy(primary = seedColor)
        }
        else -> if (darkTheme) PagesDarkColorScheme else PagesLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
