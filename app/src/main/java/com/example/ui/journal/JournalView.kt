package com.example.ui.journal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Trade
import com.example.data.model.TradeDirection
import com.example.data.model.TradeResult
import com.example.ui.TradeFilter
import com.example.ui.components.CurrencyPnlText
import com.example.ui.components.DirectionBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassTextField
import com.example.ui.components.ResultBadge
import com.example.ui.theme.LiquidTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JournalView(
    trades: List<Trade>,
    selectedFilter: TradeFilter,
    searchQuery: String,
    onFilterSelect: (TradeFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onAddTradeClick: () -> Unit,
    onEditTradeClick: (Trade) -> Unit,
    onDeleteTradeClick: (String) -> Unit
) {
    val colors = LiquidTheme.colors
    var tradeToDeleteId by remember { mutableStateOf<String?>(null) }

    val filteredTrades = trades.filter { trade ->
        val matchesFilter = when (selectedFilter) {
            TradeFilter.ALL -> true
            TradeFilter.WINS -> trade.result == TradeResult.WIN
            TradeFilter.LOSSES -> trade.result == TradeResult.LOSS
            TradeFilter.LONGS -> trade.direction == TradeDirection.LONG
            TradeFilter.SHORTS -> trade.direction == TradeDirection.SHORT
        }
        val query = searchQuery.trim().lowercase()
        val matchesSearch = if (query.isEmpty()) true else {
            trade.symbol.lowercase().contains(query) ||
                    trade.strategy.lowercase().contains(query) ||
                    trade.notes.lowercase().contains(query)
        }
        matchesFilter && matchesSearch
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("journal_view")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Search Bar
            GlassTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                label = "Search journal",
                placeholder = "Search by symbol, setup, or notes...",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                testTag = "journal_search_input"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            FilterChipsRow(
                selectedFilter = selectedFilter,
                onFilterSelect = onFilterSelect,
                totalCount = trades.size,
                winsCount = trades.count { it.result == TradeResult.WIN },
                lossesCount = trades.count { it.result == TradeResult.LOSS }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Trades List or Empty State
            if (filteredTrades.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (colors.isDark) Color(0x1A6366F1) else Color(0x1A4F46E5))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = null,
                                tint = colors.indigoAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (trades.isEmpty()) "No trades logged yet" else "No trades match filters",
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (trades.isEmpty()) "Tap the + button below to log your first trade." else "Try clearing your search query or filter.",
                            color = colors.textMuted,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 76.dp)
                        .testTag("journal_trades_list")
                ) {
                    items(filteredTrades, key = { it.id }) { trade ->
                        TradeItemCard(
                            trade = trade,
                            onEdit = { onEditTradeClick(trade) },
                            onDelete = { tradeToDeleteId = trade.id }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (tradeToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { tradeToDeleteId = null },
            title = {
                Text(text = "Delete Trade", color = colors.textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this trade? Your GM score and win rate will be automatically recalculated.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        tradeToDeleteId?.let { onDeleteTradeClick(it) }
                        tradeToDeleteId = null
                    }
                ) {
                    Text("Delete", color = colors.crimsonLoss, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tradeToDeleteId = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: TradeFilter,
    onFilterSelect: (TradeFilter) -> Unit,
    totalCount: Int,
    winsCount: Int,
    lossesCount: Int
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChipItem(
                label = "All ($totalCount)",
                isSelected = selectedFilter == TradeFilter.ALL,
                onClick = { onFilterSelect(TradeFilter.ALL) },
                testTag = "filter_all"
            )
        }
        item {
            FilterChipItem(
                label = "Wins ($winsCount)",
                isSelected = selectedFilter == TradeFilter.WINS,
                onClick = { onFilterSelect(TradeFilter.WINS) },
                testTag = "filter_wins"
            )
        }
        item {
            FilterChipItem(
                label = "Losses ($lossesCount)",
                isSelected = selectedFilter == TradeFilter.LOSSES,
                onClick = { onFilterSelect(TradeFilter.LOSSES) },
                testTag = "filter_losses"
            )
        }
        item {
            FilterChipItem(
                label = "Longs",
                isSelected = selectedFilter == TradeFilter.LONGS,
                onClick = { onFilterSelect(TradeFilter.LONGS) },
                testTag = "filter_longs"
            )
        }
        item {
            FilterChipItem(
                label = "Shorts",
                isSelected = selectedFilter == TradeFilter.SHORTS,
                onClick = { onFilterSelect(TradeFilter.SHORTS) },
                testTag = "filter_shorts"
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val colors = LiquidTheme.colors
    val bgColor = if (isSelected) colors.indigoAccent else colors.card
    val borderColor = if (isSelected) colors.indigoLight else colors.border
    val textColor = if (isSelected) Color.White else colors.textSecondary

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun TradeItemCard(
    trade: Trade,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LiquidTheme.colors
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US) }
    val formattedDate = remember(trade.timestamp) { dateFormat.format(Date(trade.timestamp)) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trade_card_${trade.id}"),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Symbol, Badges, and PnL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = trade.symbol,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    DirectionBadge(direction = trade.direction)
                    Spacer(modifier = Modifier.width(6.dp))
                    ResultBadge(result = trade.result)
                }

                CurrencyPnlText(
                    amount = trade.pnl,
                    fontSize = 17
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Row: Entry, SL, TP, R:R
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (colors.isDark) Color(0x0DFFFFFF) else Color(0x0A0F172A))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem("Entry", if (trade.entryPrice > 0) String.format(Locale.US, "$%,.2f", trade.entryPrice) else "—")
                MetricItem("Stop Loss", if (trade.stopLoss > 0) String.format(Locale.US, "$%,.2f", trade.stopLoss) else "—")
                MetricItem("Take Profit", if (trade.takeProfit > 0) String.format(Locale.US, "$%,.2f", trade.takeProfit) else "—")
                MetricItem("R:R", if (trade.riskRewardRatio > 0) String.format(Locale.US, "1 : %.2f", trade.riskRewardRatio) else "—")
            }

            // Strategy & Notes
            if (trade.strategy.isNotEmpty() || trade.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (trade.strategy.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.indigoDark.copy(alpha = 0.3f))
                                .border(1.dp, colors.indigoLight.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = trade.strategy,
                                color = colors.indigoLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (trade.notes.isNotEmpty()) {
                        Text(
                            text = trade.notes,
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Date & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    color = colors.textMuted,
                    fontSize = 11.sp
                )

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit trade",
                            tint = colors.indigoLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete trade",
                            tint = colors.crimsonLoss,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    val colors = LiquidTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = colors.textMuted, fontSize = 10.sp)
        Text(text = value, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}
