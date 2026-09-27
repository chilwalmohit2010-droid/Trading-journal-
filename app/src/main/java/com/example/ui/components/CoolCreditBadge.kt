package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LiquidTheme

/**
 * Stylish, futuristic glowing credit badge displayed at the edge of the app.
 * Fulfills: "give me cradit in app , in a edge of app show my name made by mohit in cool font"
 */
@Composable
fun CoolCreditBadge(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mohit_credit_glow")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_float"
    )

    val neonGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF818CF8), // Electric Indigo
            Color(0xFF38BDF8), // Cyan Neon
            Color(0xFF34D399), // Emerald Glow
            Color(0xFFF472B6)  // Cyberpunk Pink
        ),
        start = androidx.compose.ui.geometry.Offset(shimmerOffset * 100f, 0f),
        end = androidx.compose.ui.geometry.Offset(shimmerOffset * 300f + 200f, 100f)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0x22818CF8),
                        Color(0x2234D399)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = neonGradient,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 9.dp, vertical = 4.dp)
            .testTag("made_by_mohit_credit"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF67E8F9),
                modifier = Modifier
                    .size(11.dp)
                    .graphicsLayer { rotationZ = shimmerOffset * 360f }
            )
            Spacer(modifier = Modifier.width(4.5.dp))
            Text(
                text = "made by mohit",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontStyle = FontStyle.Italic,
                letterSpacing = 1.2.sp,
                color = Color(0xFFE0E7FF)
            )
        }
    }
}
