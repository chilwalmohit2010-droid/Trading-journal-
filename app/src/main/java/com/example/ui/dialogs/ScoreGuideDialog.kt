package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Security
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.LiquidTheme

@Composable
fun ScoreGuideDialog(
    onDismiss: () -> Unit
) {
    val colors = LiquidTheme.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Consume clicks inside */ }
                    )
                    .testTag("dialog_score_guide"),
                shape = RoundedCornerShape(24.dp)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = colors.trophyGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GM Scoring Formula",
                            color = colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "The GM (Grandmaster) Trading Score measures execution consistency, discipline, risk management, and overall edge.",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Rules List
                ScoringRuleItem(
                    icon = Icons.Default.EmojiEvents,
                    iconColor = colors.trophyGold,
                    title = "Profile & Account Baseline",
                    points = "0 PTS Base",
                    description = "Every verified trader is ranked on the global leaderboard from day 1, building points as trades are logged."
                )

                Spacer(modifier = Modifier.height(10.dp))

                ScoringRuleItem(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    iconColor = colors.emeraldWin,
                    title = "Winning Execution",
                    points = "+30 PTS / Win",
                    description = "Rewarded for validated winning trades aligned with defined strategies."
                )

                Spacer(modifier = Modifier.height(10.dp))

                ScoringRuleItem(
                    icon = Icons.Default.Security,
                    iconColor = colors.indigoAccent,
                    title = "Risk/Reward Multiplier",
                    points = "+(RR × 5) PTS",
                    description = "High R:R execution boosts points linearly (e.g. 1:3 R:R gives +15 extra PTS)."
                )

                Spacer(modifier = Modifier.height(10.dp))

                ScoringRuleItem(
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    iconColor = colors.crimsonLoss,
                    title = "Loss Management",
                    points = "-15 PTS / Loss",
                    description = "Controlled stop losses protect capital and keep drawdown penalties modest."
                )

                Spacer(modifier = Modifier.height(20.dp))

                GlassButton(
                    text = "GOT IT",
                    onClick = onDismiss,
                    accentGradient = listOf(colors.indigoDark, colors.indigoAccent),
                    testTag = "btn_close_score_guide"
                )
            }
        }
    }
}
}

@Composable
private fun ScoringRuleItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    points: String,
    description: String
) {
    val colors = LiquidTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (colors.isDark) Color(0x14FFFFFF) else Color(0x140F172A))
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = points,
                    color = iconColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = description,
                color = colors.textSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}
