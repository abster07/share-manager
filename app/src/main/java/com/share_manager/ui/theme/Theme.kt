package com.share_manager.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Deep navy + gold accent — financial / trustworthy
val Navy900  = Color(0xFF0A0E1A)
val Navy800  = Color(0xFF111827)
val Navy700  = Color(0xFF1C2841)
val Navy600  = Color(0xFF243352)
val Gold400  = Color(0xFFE8B84B)
val Gold300  = Color(0xFFF0CE7A)
val Green400 = Color(0xFF34D399)
val Red400   = Color(0xFFF87171)
val Amber400 = Color(0xFFFBBF24)
val Surface  = Color(0xFF161D2F)
val OnSurface = Color(0xFFE2E8F0)
val Outline  = Color(0xFF2D3F5E)

private val DarkColorScheme = darkColorScheme(
    primary         = Gold400,
    onPrimary       = Navy900,
    primaryContainer= Navy600,
    onPrimaryContainer = Gold300,
    secondary       = Green400,
    onSecondary     = Navy900,
    background      = Navy900,
    onBackground    = OnSurface,
    surface         = Surface,
    onSurface       = OnSurface,
    surfaceVariant  = Navy700,
    onSurfaceVariant= Color(0xFF94A3B8),
    outline         = Outline,
    error           = Red400,
    onError         = Navy900
)

@Composable
fun MeroShareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
