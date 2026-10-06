package com.agopsagopyan.randomlauncher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import com.agopsagopyan.randomlauncher.data.ThemeMode

private val Dark = darkColorScheme(
    primary = Color(0xFFE6E6E6),
    onPrimary = Color.Black,
    surface = Color(0xFF0E0E0E),
    onSurface = Color(0xFFEDEDED),
    background = Color.Black,
    onBackground = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFB5B5B5),
)

private val Light = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color.White,
    surface = Color(0xFFF7F7F7),
    onSurface = Color(0xFF111111),
    background = Color.White,
    onBackground = Color(0xFF111111),
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF4A4A4A),
)

/** Text drawn straight on the wallpaper: always white with a soft shadow so it reads on any image. */
val OnWallpaper = TextStyle(color = Color.White, shadow = Shadow(Color(0x99000000), blurRadius = 8f))

@Composable
fun RandomLauncherTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}

/** Overlays sit above the home screen in the same Box; without this, taps in empty areas fall through to home gestures. */
fun Modifier.blockTouchesBelow(): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
}
