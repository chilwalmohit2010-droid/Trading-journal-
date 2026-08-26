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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.IndigoDark
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrophyGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreGuideDialog(
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("dialog_score_guide")
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SleekCardElevated,
            borderColor = SleekBorder,
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
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x2BFFB800))
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = TrophyGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "GM SCORE ENGINE",
                                color = TextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Discipline & Performance Formula",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                ScoreRuleItem(
                    icon = Icons.Default.TrendingUp,
                    iconColor = IndigoLight,
                    title = "Base Score & Win/Loss Weight",
                    description = "Every trader begins at 1,000 GM Points. Real winning trades award +45 pts base, while losses incur a -35 pts discipline deduction. Breakeven trades earn +5 pts for capital preservation."
                )

                Spacer(modifier = Modifier.height(12.dp))

                ScoreRuleItem(
                    icon = Icons.Default.EmojiEvents,
                    iconColor = TrophyGold,
                    title = "Risk/Reward (R:R) Multiplier",
                    description = "Trades with a Risk-to-Reward ratio ≥ 2.0 earn +15 bonus pts. Excellent setups with R:R ≥ 3.0 award +30 bonus pts. Negative skew trades (< 0.5 R:R) are penalized."
                )

                Spacer(modifier = Modifier.height(12.dp))

                ScoreRuleItem(
                    icon = Icons.Default.TrendingUp,
                    iconColor = EmeraldWin,
                    title = "Win Rate Consistency & P&L",
                    description = "Traders with a consistent Win Rate > 60% gain compounding consistency bonus points. Net positive P&L provides logarithmic alpha credit."
                )

                Spacer(modifier = Modifier.height(12.dp))

                ScoreRuleItem(
                    icon = Icons.Default.Security,
                    iconColor = CrimsonLoss,
                    title = "Anti-Farming Protection",
                    description = "Rapid burst micro-trades (< 10s intervals) suffer 75% score dampening. Zero-risk/invalid stop-loss entries do not generate score."
                )

                Spacer(modifier = Modifier.height(20.dp))

                GlassButton(
                    text = "GOT IT",
                    onClick = onDismiss,
                    accentGradient = listOf(IndigoDark, IndigoAccent)
                )
            }
        }
    }
}

@Composable
private fun ScoreRuleItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x0DFFFFFF))
            .border(1.dp, SleekBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

