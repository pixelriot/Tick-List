package com.pixelriot.ticklist.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.pixelriot.ticklist.ThemeMode
import androidx.compose.foundation.shape.RoundedCornerShape

val LocalOmarchyColors = staticCompositionLocalOf { OmarchyPresets.first().dark }

private val Sharp = RoundedCornerShape(2.dp)

@Composable
fun TickListTheme(
    themeId: String,
    mode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val preset = presetById(themeId)
    val dark = when (mode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> isSystemInDarkTheme()
    }
    val colors = if (dark) preset.dark else preset.light
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.decorView.setBackgroundColor(colors.background.toArgb())
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            secondary = colors.cyan,
            onSecondary = colors.onAccent,
            background = colors.background,
            onBackground = colors.foreground,
            surface = colors.surface,
            onSurface = colors.foreground,
            surfaceVariant = colors.backgroundAlt,
            onSurfaceVariant = colors.dim,
            error = colors.red,
            onError = colors.background,
            outline = colors.border,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            secondary = colors.cyan,
            onSecondary = colors.onAccent,
            background = colors.background,
            onBackground = colors.foreground,
            surface = colors.surface,
            onSurface = colors.foreground,
            surfaceVariant = colors.backgroundAlt,
            onSurfaceVariant = colors.dim,
            error = colors.red,
            onError = colors.background,
            outline = colors.border,
        )
    }
    val typography = Typography(
        bodyLarge = TextStyle(fontFamily = JetBrainsMono, fontSize = 16.sp, color = colors.foreground),
        bodyMedium = TextStyle(fontFamily = JetBrainsMono, fontSize = 14.sp, color = colors.foreground),
        bodySmall = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, color = colors.dim),
        titleLarge = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 20.sp),
        titleMedium = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 16.sp),
        labelLarge = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    )
    CompositionLocalProvider(LocalOmarchyColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            shapes = Shapes(
                extraSmall = Sharp,
                small = Sharp,
                medium = Sharp,
                large = RoundedCornerShape(4.dp),
                extraLarge = RoundedCornerShape(4.dp),
            ),
            content = content,
        )
    }
}
