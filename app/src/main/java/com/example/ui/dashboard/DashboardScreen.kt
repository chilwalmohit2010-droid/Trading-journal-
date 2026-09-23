package com.example.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppThemeMode
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
import com.example.ui.components.ResultBadge
import com.example.ui.components.ScoreProgressionCard
import com.example.ui.components.UserAvatar
import com.example.ui.dialogs.AddEditTradeDialog
import kotlin.math.abs
import com.example.ui.dialogs.EditProfileDialog
import com.example.ui.dialogs.ScoreGuideDialog
import com.example.ui.journal.JournalView
import com.example.ui.leaderboard.LeaderboardView
import com.example.ui.profile.ProfileView
import com.example.ui.theme.LiquidTheme
import java.util.Locale

enum class DashboardTab {
    OVERVIEW,
    JOURNAL,
    LEADERBOARD,
    PROFILE
}

@Composable
fun DashboardScreen(
    currentUser: UserProfile?,
    trades: List<Trade>,
    stats: TradingStats,
    leaderboard: List<LeaderboardEntry>,
    selectedFilter: TradeFilter,
    searchQuery: String,
    themeMode: AppThemeMode,
    isAddEditOpen: Boolean,
    tradeToEdit: Trade?,
    isEditProfileOpen: Boolean,
    isActionLoading: Boolean,
    snackbarMessage: String?,
    onFilterSelect: (TradeFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onOpenAddTrade: () -> Unit,
    onOpenEditTrade: (Trade) -> Unit,
    onCloseAddEdit: () -> Unit,
    onSaveTrade: (Trade) -> Unit,
    onDeleteTrade: (String) -> Unit,
    onOpenEditProfile: () -> Unit,
    onCloseEditProfile: () -> Unit,
    onSaveProfile: (username: String, displayName: String, bio: String, photoURL: String, onComplete: (Boolean, String?) -> Unit) -> Unit,
    onUploadPhoto: (ByteArray, (String?) -> Unit) -> Unit,
    onThemeChange: (AppThemeMode) -> Unit,
    onLogout: () -> Unit,
    onDismissSnackbar: () -> Unit
) {
    val colors = LiquidTheme.colors
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
        containerColor = colors.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.bg)
        ) {
            // Subtle ambient glows
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                colors.indigoAccent.copy(alpha = if (colors.isDark) 0.12f else 0.06f),
                                Color.Transparent
                            ),
                            radius = 700f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top Header Bar
                TopTradingHeader(
                    username = currentUser?.username ?: "Trader",
                    photoUrl = currentUser?.photoURL,
                    score = stats.currentScore,
                    rank = stats.rank,
                    onScoreGuideClick = { isScoreGuideOpen = true },
                    onProfileClick = { selectedTab = DashboardTab.PROFILE }
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
                            if (targetState.ordinal > initialState.ordinal) {
                                (slideInHorizontally(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                                    initialOffsetX = { width -> width / 6 }
                                ) + fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)))
                                    .togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                                            targetOffsetX = { width -> -width / 6 }
                                        ) + fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing))
                                    )
                            } else {
                                (slideInHorizontally(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                                    initialOffsetX = { width -> -width / 6 }
                                ) + fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)))
                                    .togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                                            targetOffsetX = { width -> width / 6 }
                                        ) + fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing))
                                    )
                            }
                        },
                        label = "tab_animated_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { tab ->
                        when (tab) {
                            DashboardTab.OVERVIEW -> OverviewTabContent(
                                stats = stats,
                                allTrades = trades,
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

                            DashboardTab.PROFILE -> currentUser?.let { user ->
                                ProfileView(
                                    user = user,
                                    stats = stats,
                                    trades = trades,
                                    themeMode = themeMode,
                                    onThemeChange = onThemeChange,
                                    onEditProfileClick = onOpenEditProfile,
                                    onPhotoSelected = { bytes ->
                                        onUploadPhoto(bytes) { uploadedUrl ->
                                            if (uploadedUrl != null) {
                                                onSaveProfile(user.username, user.displayName, user.bio, uploadedUrl) { _, _ -> }
                                            }
                                        }
                                    },
                                    onLogout = onLogout
                                )
                            }
                        }
                    }
                }

                // Sleek Floating Bottom Navigation Bar (4 tabs + centered elevated FAB)
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

    // Edit Profile Modal Dialog
    if (isEditProfileOpen && currentUser != null) {
        EditProfileDialog(
            currentUser = currentUser,
            isLoading = isActionLoading,
            onDismiss = onCloseEditProfile,
            onSaveProfile = { uname, dname, bio, photo ->
                onSaveProfile(uname, dname, bio, photo) { _, _ -> }
            },
            onUploadPhoto = onUploadPhoto
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
    photoUrl: String?,
    score: Long,
    rank: Int,
    onScoreGuideClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val colors = LiquidTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onProfileClick)
        ) {
            Box(modifier = Modifier.size(42.dp)) {
                UserAvatar(
                    photoUrl = photoUrl,
                    username = username,
                    size = 42.dp
                )

                // Online indicator
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(colors.emeraldWin)
                        .border(2.dp, colors.surface, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "RANK #$rank",
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "@$username",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Trading Score Pill
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.card)
                    .border(1.dp, colors.trophyGold.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .clickable { onScoreGuideClick() }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("btn_score_guide")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Score Guide",
                        tint = colors.trophyGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "$score PTS",
                        color = colors.trophyGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewTabContent(
    stats: TradingStats,
    allTrades: List<Trade>,
    recentTrades: List<Trade>,
    onAddTradeClick: () -> Unit,
    onViewAllJournal: () -> Unit,
    onScoreGuideClick: () -> Unit
) {
    val colors = LiquidTheme.colors
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .testTag("overview_tab")
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Hero Score & Rank Performance Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("score_hero_card"),
            shape = RoundedCornerShape(24.dp),
            borderColor = colors.borderHighlight
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TRADING SCORE",
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${stats.currentScore}",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 34.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(colors.indigoDark, colors.indigoAccent)
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "GLOBAL RANK",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "#${stats.rank}",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Win rate progress indicator
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Win Rate Performance",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f%%", stats.winRate),
                            color = colors.emeraldWin,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { (stats.winRate / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = colors.emeraldWin,
                        trackColor = if (colors.isDark) Color(0x14FFFFFF) else Color(0x140F172A)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick stats summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OverviewMiniStat(label = "Total Trades", value = "${stats.totalTrades}")
                    OverviewMiniStat(label = "Wins", value = "${stats.wins}", valueColor = colors.emeraldWin)
                    OverviewMiniStat(label = "Losses", value = "${stats.losses}", valueColor = colors.crimsonLoss)
                    OverviewMiniStat(label = "Profit Factor", value = if (stats.profitFactor > 0) String.format(Locale.US, "%.2f", stats.profitFactor) else "—")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Total Net P&L Card
        val isLossPnl = stats.totalPnl < -0.0001
        val isWinPnl = stats.totalPnl > 0.0001
        val pnlBorder = when {
            isLossPnl -> colors.crimsonLoss.copy(alpha = 0.50f)
            isWinPnl -> colors.emeraldWin.copy(alpha = 0.35f)
            else -> colors.border
        }
        val pnlGlow = when {
            isLossPnl -> colors.crimsonLoss.copy(alpha = 0.16f)
            isWinPnl -> colors.emeraldWin.copy(alpha = 0.10f)
            else -> null
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            borderColor = pnlBorder,
            glowColor = pnlGlow
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL NET P&L",
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CurrencyPnlText(
                        amount = stats.totalPnl,
                        fontSize = 24
                    )
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (stats.totalPnl >= 0) colors.emeraldWinBg else colors.crimsonLossBg)
                        .border(
                            1.dp,
                            if (stats.totalPnl >= 0) colors.emeraldWin.copy(alpha = 0.4f) else colors.crimsonLoss.copy(alpha = 0.4f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (stats.totalPnl >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = if (stats.totalPnl >= 0) "Net Profit" else "Net Loss",
                        tint = if (stats.totalPnl >= 0) colors.emeraldWin else colors.crimsonLoss,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Score Progression Line Chart (Over Time based on Firestore trades)
        ScoreProgressionCard(
            trades = allTrades,
            currentScore = stats.currentScore,
            onScoreGuideClick = onScoreGuideClick
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Recent Trades Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT TRADES",
                color = colors.textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Text(
                text = "View All (${stats.totalTrades})",
                color = colors.indigoAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(onClick = onViewAllJournal)
                    .testTag("btn_view_all_trades")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (recentTrades.isEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No trades recorded yet",
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Start tracking your setups to compute your real-time trading score.",
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    GlassButton(
                        text = "Log First Trade",
                        icon = Icons.Default.Add,
                        onClick = onAddTradeClick,
                        modifier = Modifier.fillMaxWidth(0.7f),
                        testTag = "btn_log_first_trade"
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentTrades.forEach { trade ->
                    RecentTradeRow(trade = trade)
                }
            }
        }

        Spacer(modifier = Modifier.height(84.dp))
    }
}

@Composable
private fun OverviewMiniStat(
    label: String,
    value: String,
    valueColor: Color = LiquidTheme.colors.textPrimary
) {
    val colors = LiquidTheme.colors
    Column {
        Text(
            text = label,
            color = colors.textMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun RecentTradeRow(trade: Trade) {
    val colors = LiquidTheme.colors
    val isLoss = trade.result == TradeResult.LOSS || trade.pnl < -0.0001
    val displayPnl = when (trade.result) {
        TradeResult.LOSS -> -abs(trade.pnl)
        TradeResult.WIN -> abs(trade.pnl)
        TradeResult.BREAKEVEN -> 0.0
        TradeResult.OPEN -> trade.pnl
    }
    val cardBorder = when {
        isLoss -> colors.crimsonLoss.copy(alpha = 0.50f)
        trade.result == TradeResult.WIN -> colors.emeraldWin.copy(alpha = 0.30f)
        else -> colors.border
    }
    val cardGlow = if (isLoss) colors.crimsonLoss.copy(alpha = 0.14f) else null

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        borderColor = cardBorder,
        glowColor = cardGlow
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left indicator strip for loss/win
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .background(
                        when {
                            isLoss -> colors.crimsonLoss
                            trade.result == TradeResult.WIN -> colors.emeraldWin
                            else -> Color.Transparent
                        }
                    )
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DirectionBadge(direction = trade.direction)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = trade.symbol,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = trade.strategy.ifEmpty { "Price Action" },
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ResultBadge(result = trade.result)
                    Spacer(modifier = Modifier.width(10.dp))
                    CurrencyPnlText(amount = displayPnl, fontSize = 14)
                }
            }
        }
    }
}

@Composable
private fun SleekBottomNav(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
    onFabClick: () -> Unit
) {
    val colors = LiquidTheme.colors
    var lastClickTime by remember { mutableStateOf(0L) }

    fun safeTabClick(tab: DashboardTab) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 250L) {
            lastClickTime = now
            onTabSelected(tab)
        }
    }

    fun safeFabClick() {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 300L) {
            lastClickTime = now
            onFabClick()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(78.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Frosted Glass Bar with exactly 4 tabs + centered elevated slot
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.BottomCenter)
                .shadow(
                    elevation = if (colors.isDark) 12.dp else 6.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = if (colors.isDark) Color.Black.copy(alpha = 0.5f) else Color(0x1F000000),
                    spotColor = if (colors.isDark) Color.Black.copy(alpha = 0.7f) else Color(0x29000000)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface.copy(alpha = if (colors.isDark) 0.88f else 0.96f))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            colors.border.copy(alpha = 0.8f),
                            colors.border.copy(alpha = 0.2f),
                            Color.White.copy(alpha = if (colors.isDark) 0.05f else 0.4f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 1: Overview
                SleekNavTab(
                    label = "Overview",
                    icon = Icons.Default.AutoGraph,
                    isSelected = selectedTab == DashboardTab.OVERVIEW,
                    onClick = { safeTabClick(DashboardTab.OVERVIEW) },
                    testTag = "nav_overview",
                    modifier = Modifier.weight(1f)
                )

                // Tab 2: Journal
                SleekNavTab(
                    label = "Journal",
                    icon = Icons.Default.Book,
                    isSelected = selectedTab == DashboardTab.JOURNAL,
                    onClick = { safeTabClick(DashboardTab.JOURNAL) },
                    testTag = "nav_journal",
                    modifier = Modifier.weight(1f)
                )

                // Spacer for Center Elevated FAB
                Spacer(modifier = Modifier.weight(1.1f))

                // Tab 3: Rank
                SleekNavTab(
                    label = "Rank",
                    icon = Icons.Default.EmojiEvents,
                    isSelected = selectedTab == DashboardTab.LEADERBOARD,
                    onClick = { safeTabClick(DashboardTab.LEADERBOARD) },
                    testTag = "nav_leaderboard",
                    modifier = Modifier.weight(1f)
                )

                // Tab 4: Profile
                SleekNavTab(
                    label = "Profile",
                    icon = Icons.Default.Person,
                    isSelected = selectedTab == DashboardTab.PROFILE,
                    onClick = { safeTabClick(DashboardTab.PROFILE) },
                    testTag = "nav_profile",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Center Floating Action Button with iOS spring physics
        val fabInteractionSource = remember { MutableInteractionSource() }
        val isFabPressed by fabInteractionSource.collectIsPressedAsState()
        val fabScale by animateFloatAsState(
            targetValue = if (isFabPressed) 0.92f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "fab_spring"
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    scaleX = fabScale
                    scaleY = fabScale
                }
                .size(54.dp)
                .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = colors.indigoAccent)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(colors.indigoDark, colors.indigoAccent)
                    )
                )
                .border(2.5.dp, colors.bg, RoundedCornerShape(18.dp))
                .clickable(
                    interactionSource = fabInteractionSource,
                    indication = ripple(color = Color.White),
                    onClick = { safeFabClick() }
                )
                .testTag("fab_add_trade"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Log Trade",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
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
    testTag: String,
    modifier: Modifier = Modifier
) {
    val colors = LiquidTheme.colors
    val activeColor = colors.indigoAccent
    val inactiveColor = colors.textMuted

    val tabInteractionSource = remember { MutableInteractionSource() }
    val isTabPressed by tabInteractionSource.collectIsPressedAsState()

    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(160),
        label = "nav_color"
    )
    val animatedScale by animateFloatAsState(
        targetValue = when {
            isTabPressed -> 0.94f
            isSelected -> 1.04f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav_scale"
    )

    val pillAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.0f,
        animationSpec = tween(180),
        label = "pill_alpha"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = tabInteractionSource,
                indication = ripple(color = colors.indigoLight),
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Active glass pill indicator
        if (pillAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = pillAlpha }
                    .background(
                        color = colors.indigoAccent.copy(alpha = if (colors.isDark) 0.14f else 0.12f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = colors.indigoAccent.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(14.dp)
                    )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = animatedColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label.uppercase(),
                color = animatedColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 9.5.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}
