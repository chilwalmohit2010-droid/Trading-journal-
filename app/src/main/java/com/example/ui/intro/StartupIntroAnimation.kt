package com.example.ui.intro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CoolCreditBadge
import com.example.ui.theme.LiquidTheme
import kotlinx.coroutines.delay

/**
 * Awesome Startup Intro Sequence:
 * Stage 1: Text being typed out character by character in a high-tech terminal typewriter style.
 * Stage 2: Animated boxes creating and drawing their neon wireframe outlines on screen.
 * Stage 3: Smooth, luxurious opening transition into the full app.
 */
@Composable
fun StartupIntroAnimation(
    onAnimationComplete: () -> Unit
) {
    val colors = LiquidTheme.colors

    // Animation Stages: 1 = Typewriter text, 2 = Creating Boxes, 3 = App opening reveal
    var stage by remember { mutableIntStateOf(1) }

    // Typewriter text state
    val fullText1 = "> INITIALIZING QUANT ENGINE..."
    val fullText2 = "> SYSTEM READY // TRADING DIARY"
    var typedLine1 by remember { mutableStateOf("") }
    var typedLine2 by remember { mutableStateOf("") }
    var showCursor by remember { mutableStateOf(true) }

    // Blinking cursor transition
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    // Stage 2 Box outline draw progress
    val boxDrawProgress = remember { Animatable(0f) }
    val boxScale = remember { Animatable(0.85f) }

    // Stage 3 smooth exit fade/scale
    val overallAlpha = remember { Animatable(1f) }
    val overallScale = remember { Animatable(1f) }

    // Master Timeline
    LaunchedEffect(Unit) {
        // Stage 1: Typewriter Effect
        for (i in 1..fullText1.length) {
            typedLine1 = fullText1.substring(0, i)
            delay(30)
        }
        delay(120)
        for (i in 1..fullText2.length) {
            typedLine2 = fullText2.substring(0, i)
            delay(28)
        }
        delay(350)

        // Stage 2: Creating Boxes Animation
        stage = 2
        boxScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        boxDrawProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(750, easing = FastOutSlowInEasing)
        )
        delay(550)

        // Stage 3: Smooth opening transition into the main app
        stage = 3
        overallScale.animateTo(
            targetValue = 1.05f,
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        )
        overallAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(300, easing = LinearEasing)
        )

        onAnimationComplete()
    }

    // Tap to skip
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .graphicsLayer {
                alpha = overallAlpha.value
                scaleX = overallScale.value
                scaleY = overallScale.value
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onAnimationComplete
            )
            .testTag("startup_intro_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Cyber ambient radial glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colors.indigoAccent.copy(alpha = 0.16f),
                            colors.emeraldWin.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        radius = 900f
                    )
                )
        )

        // Top Edge: "made by mohit" Credit Badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, end = 20.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            CoolCreditBadge()
        }

        // Center Content depending on stage
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Stage 1: Terminal Typewriter Text
            AnimatedVisibility(
                visible = stage == 1,
                enter = fadeIn(),
                exit = fadeOut(tween(200))
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x1A0F172A))
                        .border(1.dp, colors.indigoAccent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "terminal.exe",
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = typedLine1,
                        color = Color(0xFF67E8F9),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = typedLine2,
                            color = Color(0xFF34D399),
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " █",
                            color = colors.emeraldWin.copy(alpha = cursorAlpha),
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Stage 2 & 3: Creating Boxes Animation (Blueprint wireframe assembling)
            AnimatedVisibility(
                visible = stage >= 2,
                enter = fadeIn(tween(250)) + scaleIn(tween(350)),
                exit = fadeOut(tween(200))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .graphicsLayer {
                            scaleX = boxScale.value
                            scaleY = boxScale.value
                        }
                ) {
                    Text(
                        text = "CONSTRUCTING INTERFACE...",
                        color = colors.indigoAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 1. Box 1: Trading Score Box (Creating box animation)
                    AnimatedBoxContainer(
                        progress = boxDrawProgress.value,
                        title = "METRICS ENGINE",
                        subtitle = "Risk Matrix • Score Calculator Active",
                        accentColor = colors.indigoAccent
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Box 2: Candlestick / Chart Box (Creating box animation)
                    AnimatedBoxContainer(
                        progress = boxDrawProgress.value,
                        title = "CANVAS GRAPH CORE",
                        subtitle = "High Refresh Rate Line Renderer",
                        accentColor = colors.emeraldWin
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Box 3: Journal Storage Box (Creating box animation)
                    AnimatedBoxContainer(
                        progress = boxDrawProgress.value,
                        title = "LOCAL VAULT & CLOUD",
                        subtitle = "Room Persistence • 100% Offline Capable",
                        accentColor = Color(0xFF38BDF8)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Tap anywhere to enter instantly",
                        color = colors.textMuted.copy(alpha = 0.7f),
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedBoxContainer(
    progress: Float,
    title: String,
    subtitle: String,
    accentColor: Color
) {
    val colors = LiquidTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x180F172A))
    ) {
        // Animated Canvas drawing the box borders
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 2.dp.toPx()
            val w = size.width
            val h = size.height
            val totalPerimeter = 2 * (w + h)
            val currentLen = totalPerimeter * progress

            // Draw glowing path border
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.85f),
                        Color(0xFF818CF8).copy(alpha = 0.6f)
                    )
                ),
                size = Size(w, h),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(
                    width = strokeW,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(currentLen, totalPerimeter),
                        0f
                    )
                )
            )
        }

        // Box Content inside
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = colors.textMuted,
                    fontSize = 11.sp
                )
            }

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = accentColor.copy(alpha = progress.coerceIn(0f, 1f)),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
