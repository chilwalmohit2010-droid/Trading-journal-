package com.example.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeaderboardEntry
import com.example.data.model.Trade
import com.example.data.model.TradeResult
import com.example.data.model.TradingStats
import com.example.data.model.UserProfile
import com.example.ui.TradeFilter
import com.example.ui.components.CurrencyPnlText
import com.example.ui.components.DirectionBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.dialogs.AddEditTradeDialog
import com.example.ui.dialogs.ScoreGuideDialog
import com.example.ui.journal.JournalView
import com.example.ui.leaderboard.LeaderboardView
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldSolid
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.EmeraldWinBg
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.IndigoDark
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.OrangeStreak
import com.example.ui.theme.SleekBg
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderHighlight
import com.example.ui.theme.SleekBorderSubtle
import com.example.ui.theme.SleekCard
import com.example.ui.theme.SleekCardElevated
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrophyGold
import java.util.Locale

enum class DashboardTab {
    OVERVIEW,
    JOURNAL,
    LEADERBOARD
}

@Composable
fun DashboardScreen(
    currentUser: UserProfile?,
    trades: List<Trade>,
    stats: TradingStats,
    leaderboard: List<LeaderboardEntry>,
    selectedFilter: TradeFilter,
    searchQuery: String,
    isAddEditOpen: Boolean,
    tradeToEdit: Trade?,
    isActionLoading: Boolean,
    snackbarMessage: String?,
    onFilterSelect: (TradeFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onOpenAddTrade: () -> Unit,
    onOpenEditTrade: (Trade) -> Unit,
    onCloseAddEdit: () -> Unit,
    onSaveTrade: (Trade) -> Unit,
    onDeleteTrade: (String) -> Unit,
    onLogout: () -> Unit,
    onDismissSnackbar: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(DashboardTab.OVERVIEW) }
    var isScoreGuideOpen by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        if (!snackbarMessage.isNullOrEmpty()) {
            snackbarHostState.showSnackbar(snackbarMessage)
            onDismissSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SleekBg,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SleekBg)
        ) {
            // Subtle ambient glows from top and corners
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0x1A6366F1), Color.Transparent),
                            radius = 700f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Sleek Top App Bar
                TopTradingHeader(
                    username = currentUser?.username ?: "Trader",
                    score = stats.currentScore,
                    rank = stats.rank,
                    onScoreGuideClick = { isScoreGuideOpen = true },
                    onLogout = onLogout
                )

                // Tab Content Switcher
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)) togetherWith
                                    fadeOut(animationSpec = tween(150))
                        },
                        label = "tab_content_animation"
                    ) { tab ->
                        when (tab) {
                            DashboardTab.OVERVIEW -> OverviewTabContent(
                                stats = stats,
                                recentTrades = trades.take(5),
                                onAddTradeClick = onOpenAddTrade,
                                onViewAllJournal = { selectedTab = DashboardTab.JOURNAL },
                                onScoreGuideClick = { isScoreGuideOpen = true }
                            )

                            DashboardTab.JOURNAL -> JournalView(
                                trades = trades,
                                selectedFilter = selectedFilter,
                                searchQuery = searchQuery,
                                onFilterSelect = onFilterSelect,
                                onSearchChange = onSearchChange,
                                onAddTradeClick = onOpenAddTrade,
                                onEditTradeClick = onOpenEditTrade,
                                onDeleteTradeClick = onDeleteTrade
                            )

                            DashboardTab.LEADERBOARD -> LeaderboardView(
                                currentUser = currentUser,
                                stats = stats,
                                leaderboard = leaderboard,
                                onScoreGuideClick = { isScoreGuideOpen = true }
                            )
                        }
                    }
                }

                // Sleek Floating Bottom Navigation Bar with Diamond FAB
                SleekBottomNav(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onFabClick = onOpenAddTrade
                )
            }
        }
    }

    // Add / Edit Trade Modal Dialog
    if (isAddEditOpen) {
        AddEditTradeDialog(
            trade = tradeToEdit,
            isLoading = isActionLoading,
            onDismiss = onCloseAddEdit,
            onSave = onSaveTrade
        )
    }

    // Score Guide Modal Dialog
    if (isScoreGuideOpen) {
        ScoreGuideDialog(onDismiss = { isScoreGuideOpen = false })
    }
}

@Composable
private fun TopTradingHeader(
    username: String,
    score: Long,
    rank: Int,
    onScoreGuideClick: () -> Unit,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // User Avatar with multi-color gradient + online dot
            Box(modifier = Modifier.size(44.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(IndigoAccent, BlueAccent, EmeraldAccent)
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Text(
                        text = username.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp
                    )
                }

                // Online indicator
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(EmeraldSolid)
                        .border(2.dp, SleekBg, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "RANK #$rank",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = username,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // GM Score Pill
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SleekCard)
                    .border(1.dp, TrophyGold.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .clickable { onScoreGuideClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("btn_score_guide")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Score Guide",
                        tint = TrophyGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$score PTS",
                        color = TrophyGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Logout Button
            IconButton(
                onClick = onLogout,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SleekCard)
                    .border(1.dp, SleekBorder, CircleShape)
                    .testTag("btn_logout")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Log Out",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun OverviewTabContent(
    stats: TradingStats,
    recentTrades: List<Trade>,
    onAddTradeClick: () -> Unit,
    onViewAllJournal: () -> Unit,
    onScoreGuideClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .testTag("overview_tab_content")
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // Sleek 2x2 Metrics Overview Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Total Realized Net P&L
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp),
                backgroundColor = SleekCard
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL NET P&L",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = if (stats.totalPnl >= 0) EmeraldWin else CrimsonLoss,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    CurrencyPnlText(
                        amount = stats.totalPnl,
                        fontSize = 20
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (stats.totalPnl >= 0) EmeraldWinBg else Color(0x1AF43F5E))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (stats.profitFactor >= 99) "PF: ∞" else String.format(Locale.US, "PF: %.2f", stats.profitFactor),
                            color = if (stats.totalPnl >= 0) EmeraldWin else CrimsonLoss,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Card 2: Win Rate
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp),
                backgroundColor = SleekCard
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WIN RATE",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = IndigoLight,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = String.format(Locale.US, "%.1f%%", stats.winRate),
                        color = IndigoLight,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )

                    Text(
                        text = "${stats.wins} Wins / ${stats.losses} Losses",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 3: GM Trader Score
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
                    .clickable { onScoreGuideClick() },
                backgroundColor = SleekCard
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GM SCORE",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = TrophyGold,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = "${stats.currentScore}",
                        color = TrophyGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )

                    // Subtle glowing score progress indicator
                    val scoreFraction = ((stats.currentScore - 1000).coerceAtLeast(0) % 1000) / 1000f
                    LinearProgressIndicator(
                        progress = { if (scoreFraction == 0f && stats.currentScore >= 1000) 0.5f else scoreFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = TrophyGold,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }

            // Card 4: Total Trades & Discipline
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp),
                backgroundColor = SleekCard
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL TRADES",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = OrangeStreak,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = "${stats.totalTrades}",
                        color = OrangeStreak,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )

                    Text(
                        text = if (stats.avgRiskReward > 0) String.format(Locale.US, "Avg R:R 1:%.2f", stats.avgRiskReward) else "Discipline Active",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Main Container / Curved Activity Section
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SleekCardElevated,
            borderColor = SleekBorder,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT ACTIVITY",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = "View All >",
                        color = IndigoLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onViewAllJournal() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (recentTrades.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No trades recorded yet", color = TextSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        GlassButton(
                            text = "+ Log First Trade",
                            onClick = onAddTradeClick,
                            modifier = Modifier.width(180.dp)
                        )
                    }
                } else {
                    recentTrades.forEach { trade ->
                        RecentTradeRow(trade = trade)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}

@Composable
private fun RecentTradeRow(trade: Trade) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0DFFFFFF))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DirectionBadge(direction = trade.direction)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = trade.symbol,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = trade.result.name,
                        color = when (trade.result) {
                            TradeResult.WIN -> EmeraldWin
                            TradeResult.LOSS -> CrimsonLoss
                            TradeResult.BREAKEVEN -> TextSecondary
                            TradeResult.OPEN -> TrophyGold
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            CurrencyPnlText(
                amount = trade.pnl,
                fontSize = 14
            )
        }
    }
}

@Composable
private fun SleekBottomNav(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
    onFabClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Bottom Bar Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color(0xFA050608))
                .border(1.dp, SleekBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Tabs: Dashboard
                SleekNavTab(
                    label = "Dash",
                    icon = Icons.Default.Timeline,
                    isSelected = selectedTab == DashboardTab.OVERVIEW,
                    onClick = { onTabSelected(DashboardTab.OVERVIEW) },
                    testTag = "nav_overview"
                )

                // Journal
                SleekNavTab(
                    label = "Journal",
                    icon = Icons.Default.Book,
                    isSelected = selectedTab == DashboardTab.JOURNAL,
                    onClick = { onTabSelected(DashboardTab.JOURNAL) },
                    testTag = "nav_journal"
                )

                Spacer(modifier = Modifier.width(48.dp)) // Space for center FAB

                // Leaderboard
                SleekNavTab(
                    label = "Rank",
                    icon = Icons.Default.EmojiEvents,
                    isSelected = selectedTab == DashboardTab.LEADERBOARD,
                    onClick = { onTabSelected(DashboardTab.LEADERBOARD) },
                    testTag = "nav_leaderboard"
                )
            }
        }

        // Diamond Center Floating Action Button (-mt-12)
        Box(
            modifier = Modifier
                .offset(y = (-20).dp)
                .size(52.dp)
                .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = IndigoAccent)
                .clip(RoundedCornerShape(16.dp))
                .background(IndigoDark)
                .border(3.dp, SleekBg, RoundedCornerShape(16.dp))
                .clickable(onClick = onFabClick)
                .testTag("fab_add_trade"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Log Trade",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun SleekNavTab(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val activeColor = IndigoLight
    val inactiveColor = TextMuted

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label.uppercase(),
            color = if (isSelected) activeColor else inactiveColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp
        )
    }
}
