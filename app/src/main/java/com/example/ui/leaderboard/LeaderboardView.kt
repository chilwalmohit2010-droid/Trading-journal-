package com.example.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeaderboardEntry
import com.example.data.model.TradingStats
import com.example.data.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.components.RankBadge
import com.example.ui.theme.BronzeRank
import com.example.ui.theme.EmeraldSolid
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.IndigoDark
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.SilverRank
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekCard
import com.example.ui.theme.SleekCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrophyGold
import java.util.Locale

@Composable
fun LeaderboardView(
    currentUser: UserProfile?,
    stats: TradingStats,
    leaderboard: List<LeaderboardEntry>,
    onScoreGuideClick: () -> Unit
) {
    // Top 10 sorted entries
    val top10 = leaderboard.take(10)
    val currentUid = currentUser?.uid ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("leaderboard_view")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Current User Rank Banner (Pinned)
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("user_rank_banner"),
            backgroundColor = SleekCard,
            borderColor = IndigoAccent.copy(alpha = 0.4f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RankBadge(rank = stats.rank)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "YOU (${currentUser?.username ?: "Trader"})",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (stats.rank <= 10) "Top 10 Global GM Trader" else "Global Standing: Rank #${stats.rank}",
                            color = IndigoLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${stats.currentScore} PTS",
                        color = TrophyGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f%% WR • %d trades", stats.winRate, stats.totalTrades),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Header & Live Sync Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = TrophyGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "GLOBAL TOP 10 LEADERBOARD",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(EmeraldSolid)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Live Sync",
                    color = EmeraldWin,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Top 10 List or Empty State
        if (top10.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Text(
                    text = "No ranked traders yet. Add your trades to claim #1!",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 72.dp)
                    .testTag("top_10_list")
            ) {
                itemsIndexed(top10, key = { index, item -> item.uid.ifEmpty { "rank_$index" } }) { index, entry ->
                    val rank = index + 1
                    val isCurrentUser = entry.uid == currentUid

                    LeaderboardRow(
                        rank = rank,
                        entry = entry,
                        isCurrentUser = isCurrentUser
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Privacy guarantee notice
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x0DFFFFFF))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy Protected: Only usernames & verified scores are shared. Private trades and balances remain confidential.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    rank: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean
) {
    val borderColor = if (isCurrentUser) IndigoAccent.copy(alpha = 0.6f) else SleekBorder
    val bgColor = if (isCurrentUser) Color(0x1F6366F1) else SleekCard

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_row_$rank"),
        backgroundColor = bgColor,
        borderColor = borderColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RankBadge(rank = rank)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.username.ifEmpty { "Trader #${entry.uid.take(4)}" },
                            color = if (isCurrentUser) IndigoLight else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(IndigoDark)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text("YOU", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Text(
                        text = "${entry.totalTrades} trades logged",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.score} PTS",
                    color = when (rank) {
                        1 -> TrophyGold
                        2 -> SilverRank
                        3 -> BronzeRank
                        else -> TextPrimary
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
                if (entry.winRate > 0) {
                    Text(
                        text = String.format(Locale.US, "%.0f%% Win Rate", entry.winRate),
                        color = EmeraldWin,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

