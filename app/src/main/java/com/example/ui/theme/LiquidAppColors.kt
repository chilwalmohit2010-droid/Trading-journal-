package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class LiquidAppColors(
    val bg: Color,
    val surface: Color,
    val card: Color,
    val cardElevated: Color,
    val border: Color,
    val borderSubtle: Color,
    val borderHighlight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val indigoAccent: Color = Color(0xFF6366F1),
    val indigoDark: Color = Color(0xFF4F46E5),
    val indigoLight: Color = Color(0xFF818CF8),
    val emeraldWin: Color = Color(0xFF34D399),
    val emeraldWinBg: Color = Color(0x1A34D399),
    val crimsonLoss: Color = Color(0xFFF43F5E),
    val crimsonLossBg: Color = Color(0x1AF43F5E),
    val trophyGold: Color = Color(0xFFEAB308),
    val trophyGoldBg: Color = Color(0x26EAB308),
    val isDark: Boolean = true
)

val DarkLiquidColors = LiquidAppColors(
    bg = Color(0xFF07090E),
    surface = Color(0xFF0D111A),
    card = Color(0x14FFFFFF), // Frosted white/8% on dark
    cardElevated = Color(0x1FFFFFFF), // Frosted white/12% on dark
    border = Color(0x1FFFFFFF), // Subtle glass border
    borderSubtle = Color(0x0FFFFFFF),
    borderHighlight = Color(0x4D6366F1),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    indigoAccent = Color(0xFF6366F1),
    indigoDark = Color(0xFF4F46E5),
    indigoLight = Color(0xFF818CF8),
    emeraldWin = Color(0xFF34D399),
    emeraldWinBg = Color(0x1A34D399),
    crimsonLoss = Color(0xFFF43F5E),
    crimsonLossBg = Color(0x1AF43F5E),
    trophyGold = Color(0xFFEAB308),
    trophyGoldBg = Color(0x26EAB308),
    isDark = true
)

val LightLiquidColors = LiquidAppColors(
    bg = Color(0xFFF1F5F9), // Crisp soft slate 100
    surface = Color(0xFFFFFFFF),
    card = Color(0xEBFFFFFF), // Translucent clean white glass 92%
    cardElevated = Color(0xF7FFFFFF), // Translucent clean white glass 97%
    border = Color(0x260F172A), // Crisp subtle slate glass border
    borderSubtle = Color(0x140F172A),
    borderHighlight = Color(0x666366F1),
    textPrimary = Color(0xFF0F172A), // Slate 900
    textSecondary = Color(0xFF475569), // Slate 600
    textMuted = Color(0xFF64748B), // Slate 500
    indigoAccent = Color(0xFF4F46E5), // Indigo 600 for high light contrast
    indigoDark = Color(0xFF4338CA),
    indigoLight = Color(0xFF6366F1),
    emeraldWin = Color(0xFF059669), // Emerald 600
    emeraldWinBg = Color(0x2610B981),
    crimsonLoss = Color(0xFFE11D48), // Rose 600
    crimsonLossBg = Color(0x26F43F5E),
    trophyGold = Color(0xFFD97706), // Amber 600
    trophyGoldBg = Color(0x26F59E0B),
    isDark = false
)

val LocalLiquidColors = staticCompositionLocalOf { DarkLiquidColors }

object LiquidTheme {
    val colors: LiquidAppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLiquidColors.current
}
