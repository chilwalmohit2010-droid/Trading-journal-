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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
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
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.IndigoDark
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekCard
import com.example.ui.theme.SleekCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrophyGold
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
    var tradeToDeleteId by remember { mutableStateOf<String?>(null) }

    // Filter and search computation
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            GlassTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                label = "Search Journal",
                placeholder = "Search by pair (BTC), strategy or notes...",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary
                    )
                },
                testTag = "input_journal_search"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(TradeFilter.values()) { filter ->
                    val isSelected = filter == selectedFilter
                    val label = when (filter) {
                        TradeFilter.ALL -> "All Trades (${trades.size})"
                        TradeFilter.WINS -> "Wins (${trades.count { it.result == TradeResult.WIN }})"
                        TradeFilter.LOSSES -> "Losses (${trades.count { it.result == TradeResult.LOSS }})"
                        TradeFilter.LONGS -> "Longs (${trades.count { it.direction == TradeDirection.LONG }})"
                        TradeFilter.SHORTS -> "Shorts (${trades.count { it.direction == TradeDirection.SHORT }})"
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) IndigoDark else Color(0x0DFFFFFF))
                            .border(
                                1.dp,
                                if (isSelected) IndigoLight else SleekBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onFilterSelect(filter) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("filter_chip_${filter.name.lowercase()}")
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trades List or Empty State
            if (filteredTrades.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp)
                ) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        backgroundColor = SleekCard,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1A6366F1))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShowChart,
                                    contentDescription = null,
                                    tint = IndigoLight,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty() || selectedFilter != TradeFilter.ALL)
                                    "No matching trades found"
                                else
                                    "Your Journal is Empty",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Log every trade with Entry, SL, TP, and P&L to build your GM Score.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp),
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.horizontalGradient(listOf(IndigoDark, IndigoAccent)))
                                    .clickable { onAddTradeClick() }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                                    .testTag("btn_empty_add_trade")
                            ) {
                                Text(
                                    text = "+ LOG FIRST TRADE",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 72.dp)
                        .testTag("trades_list")
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
            title = { Text("Delete Trade Entry", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove this trade? Your GM score will be recalculated.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        tradeToDeleteId?.let { onDeleteTradeClick(it) }
                        tradeToDeleteId = null
                    }
                ) {
                    Text("DELETE", color = CrimsonLoss, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tradeToDeleteId = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = SleekCardElevated,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
private fun TradeItemCard(
    trade: Trade,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US) }
    val formattedDate = remember(trade.timestamp) { dateFormat.format(Date(trade.timestamp)) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trade_card_${trade.id}"),
        backgroundColor = SleekCard,
        borderColor = SleekBorder,
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
                        color = TextPrimary,
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
                    .background(Color(0x0DFFFFFF))
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
                                .background(IndigoDark.copy(alpha = 0.5f))
                                .border(1.dp, IndigoLight.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = trade.strategy,
                                color = IndigoLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (trade.notes.isNotEmpty()) {
                        Text(
                            text = trade.notes,
                            color = TextSecondary,
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
                    color = TextMuted,
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
                            tint = IndigoLight,
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
                            tint = CrimsonLoss,
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextMuted, fontSize = 10.sp)
        Text(text = value, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

