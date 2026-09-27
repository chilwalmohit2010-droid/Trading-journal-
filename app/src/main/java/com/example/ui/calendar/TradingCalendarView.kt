package com.example.ui.calendar

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Trade
import com.example.data.model.TradeResult
import com.example.ui.components.CurrencyPnlText
import com.example.ui.components.DirectionBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.ResultBadge
import com.example.ui.theme.LiquidTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class DailyHeatmapData(
    val dayOfMonth: Int,
    val dateKey: String, // "yyyy-MM-dd"
    val trades: List<Trade>,
    val netPnl: Double,
    val winCount: Int,
    val lossCount: Int
)

/**
 * Interactive Monthly Trading Calendar & Daily Heatmap.
 * Shows Green (profit) and Red (loss) days with daily P&L and trade counts
 * just like professional proprietary trading firm dashboards (FTMO, Topstep).
 */
@Composable
fun TradingCalendarView(
    trades: List<Trade>,
    onTradeClick: (Trade) -> Unit = {},
    onTradeChartClick: (Trade) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LiquidTheme.colors

    // Currently displayed month state (default to current month)
    var displayedCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }

    // Selected day for drilldown detail
    var selectedDayKey by remember { mutableStateOf<String?>(null) }

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.US) }
    val dayKeyFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val dayDisplayFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.US) }

    val displayedYear = displayedCalendar.get(Calendar.YEAR)
    val displayedMonth = displayedCalendar.get(Calendar.MONTH)

    // Group trades by "yyyy-MM-dd"
    val tradesByDay by remember(trades) {
        derivedStateOf {
            val map = mutableMapOf<String, MutableList<Trade>>()
            trades.forEach { trade ->
                val key = dayKeyFormat.format(Date(trade.timestamp))
                map.getOrPut(key) { mutableListOf() }.add(trade)
            }
            map
        }
    }

    // Days in current month calculation
    val daysInMonth by remember(displayedCalendar) {
        derivedStateOf {
            val cal = displayedCalendar.clone() as Calendar
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sun, 1 = Mon ...
            val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            Pair(firstDayOfWeek, totalDays)
        }
    }

    // Monthly aggregates
    val monthlyStats by remember(trades, displayedYear, displayedMonth) {
        derivedStateOf {
            var netPnl = 0.0
            var monthlyTradesCount = 0
            var greenDays = 0
            var redDays = 0
            var breakEvenDays = 0
            var bestDayPnl = Double.NEGATIVE_INFINITY
            var worstDayPnl = Double.POSITIVE_INFINITY

            val cal = Calendar.getInstance()
            tradesByDay.forEach { (dateKey, dayTrades) ->
                try {
                    val date = dayKeyFormat.parse(dateKey)
                    if (date != null) {
                        cal.time = date
                        if (cal.get(Calendar.YEAR) == displayedYear && cal.get(Calendar.MONTH) == displayedMonth) {
                            val dayPnl = dayTrades.sumOf { it.effectivePnl }
                            netPnl += dayPnl
                            monthlyTradesCount += dayTrades.size
                            if (dayPnl > 0.01) {
                                greenDays++
                                if (dayPnl > bestDayPnl) bestDayPnl = dayPnl
                            } else if (dayPnl < -0.01) {
                                redDays++
                                if (dayPnl < worstDayPnl) worstDayPnl = dayPnl
                            } else {
                                breakEvenDays++
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore parse error
                }
            }

            object {
                val net = netPnl
                val count = monthlyTradesCount
                val green = greenDays
                val red = redDays
                val winDayRate = if (greenDays + redDays > 0) (greenDays * 100) / (greenDays + redDays) else 0
                val best = if (bestDayPnl != Double.NEGATIVE_INFINITY) bestDayPnl else 0.0
                val worst = if (worstDayPnl != Double.POSITIVE_INFINITY) worstDayPnl else 0.0
            }
        }
    }

    // Selected day trades
    val selectedDayTrades = remember(selectedDayKey, tradesByDay) {
        if (selectedDayKey != null) tradesByDay[selectedDayKey] ?: emptyList() else emptyList()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("trading_calendar_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Month Navigation Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = colors.indigoAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = monthYearFormat.format(displayedCalendar.time).uppercase(Locale.US),
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Previous Month Button
                    IconButton(
                        onClick = {
                            val nextCal = displayedCalendar.clone() as Calendar
                            nextCal.add(Calendar.MONTH, -1)
                            displayedCalendar = nextCal
                            selectedDayKey = null
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (colors.isDark) Color(0x18FFFFFF) else Color(0x0C000000))
                            .testTag("btn_prev_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // "Today" quick button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (colors.isDark) Color(0x18FFFFFF) else Color(0x0C000000))
                            .clickable {
                                displayedCalendar = Calendar.getInstance().apply {
                                    set(Calendar.DAY_OF_MONTH, 1)
                                }
                                val todayKey = dayKeyFormat.format(Date())
                                selectedDayKey = todayKey
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Today",
                            color = colors.indigoLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Next Month Button
                    IconButton(
                        onClick = {
                            val nextCal = displayedCalendar.clone() as Calendar
                            nextCal.add(Calendar.MONTH, 1)
                            displayedCalendar = nextCal
                            selectedDayKey = null
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (colors.isDark) Color(0x18FFFFFF) else Color(0x0C000000))
                            .testTag("btn_next_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Monthly Summary Bar: Net PnL, Green/Red days, Total trades
        item {
            val isMonthGreen = monthlyStats.net >= 0
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                borderColor = if (isMonthGreen) colors.emeraldWin.copy(alpha = 0.35f) else colors.crimsonLoss.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MONTHLY NET P&L",
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        CurrencyPnlText(
                            amount = monthlyStats.net,
                            fontSize = 20
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Green days pill
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.emeraldWin)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${monthlyStats.green}W",
                                color = colors.emeraldWin,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Red days pill
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.crimsonLoss)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${monthlyStats.red}L",
                                color = colors.crimsonLoss,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Total trades
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.card)
                                .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${monthlyStats.count} trades",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Calendar Heatmap Grid Card
        item {
            val (firstDayOfWeek, totalDays) = daysInMonth
            val dayHeaders = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
            val totalCells = firstDayOfWeek + totalDays
            val totalWeeks = (totalCells + 6) / 7

            val todayCalendar = Calendar.getInstance()
            val isCurrentMonth = todayCalendar.get(Calendar.YEAR) == displayedYear && todayCalendar.get(Calendar.MONTH) == displayedMonth
            val todayDay = todayCalendar.get(Calendar.DAY_OF_MONTH)

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Day Headers (Sun - Sat)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        dayHeaders.forEach { header ->
                            Text(
                                text = header,
                                color = colors.textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Weeks & Days Grid
                    for (week in 0 until totalWeeks) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (dayCol in 0..6) {
                                val cellIndex = week * 7 + dayCol
                                val dayNumber = cellIndex - firstDayOfWeek + 1

                                if (dayNumber in 1..totalDays) {
                                    val dateKey = String.format(Locale.US, "%04d-%02d-%02d", displayedYear, displayedMonth + 1, dayNumber)
                                    val dayTrades = tradesByDay[dateKey] ?: emptyList()
                                    val dayNet = dayTrades.sumOf { it.effectivePnl }
                                    val isSelected = selectedDayKey == dateKey
                                    val isToday = isCurrentMonth && dayNumber == todayDay

                                    CalendarDayCell(
                                        dayNumber = dayNumber,
                                        tradeCount = dayTrades.size,
                                        netPnl = dayNet,
                                        isToday = isToday,
                                        isSelected = isSelected,
                                        onClick = {
                                            selectedDayKey = if (isSelected) null else dateKey
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    // Empty day slot outside month range
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Drilldown Details Drawer
        if (selectedDayKey != null) {
            item {
                val formattedHeaderDate = try {
                    val parsed = dayKeyFormat.parse(selectedDayKey!!)
                    if (parsed != null) dayDisplayFormat.format(parsed) else selectedDayKey!!
                } catch (e: Exception) {
                    selectedDayKey!!
                }
                val dayPnl = selectedDayTrades.sumOf { it.effectivePnl }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    borderColor = if (dayPnl >= 0) colors.emeraldWin.copy(alpha = 0.45f) else colors.crimsonLoss.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = colors.indigoAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = formattedHeaderDate,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${selectedDayTrades.size} trades logged",
                                        color = colors.textMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CurrencyPnlText(amount = dayPnl, fontSize = 16)
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { selectedDayKey = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        if (selectedDayTrades.isEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No trades were executed on this day.",
                                color = colors.textMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // List of trades on the selected day
            items(selectedDayTrades, key = { "day_${it.id}" }) { trade ->
                CalendarTradeItemCard(
                    trade = trade,
                    onClick = { onTradeClick(trade) },
                    onChartClick = { onTradeChartClick(trade) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    tradeCount: Int,
    netPnl: Double,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidTheme.colors

    val hasTrades = tradeCount > 0
    val isWin = hasTrades && netPnl > 0.01
    val isLoss = hasTrades && netPnl < -0.01

    val cellBg = when {
        isSelected -> colors.indigoAccent.copy(alpha = 0.28f)
        isWin -> colors.emeraldWin.copy(alpha = if (colors.isDark) 0.20f else 0.14f)
        isLoss -> colors.crimsonLoss.copy(alpha = if (colors.isDark) 0.20f else 0.14f)
        hasTrades -> colors.card.copy(alpha = 0.6f)
        else -> Color.Transparent
    }

    val cellBorder = when {
        isSelected -> colors.indigoLight
        isToday -> colors.trophyGold.copy(alpha = 0.8f)
        isWin -> colors.emeraldWin.copy(alpha = 0.45f)
        isLoss -> colors.crimsonLoss.copy(alpha = 0.45f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .padding(2.dp)
            .height(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(cellBg)
            .border(
                width = if (isSelected || isToday) 1.5.dp else if (hasTrades) 1.dp else 0.dp,
                color = cellBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Day number + today dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = dayNumber.toString(),
                    color = if (hasTrades) colors.textPrimary else colors.textMuted.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = if (hasTrades || isToday) FontWeight.Black else FontWeight.Normal
                )
                if (isToday) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(colors.trophyGold)
                    )
                }
            }

            // PnL or trade count
            if (hasTrades) {
                val formattedAmount = when {
                    abs(netPnl) >= 1000 -> String.format(Locale.US, "$%.0fk", netPnl / 1000.0)
                    abs(netPnl) >= 1 -> String.format(Locale.US, "$%.0f", netPnl)
                    else -> String.format(Locale.US, "$%.1f", netPnl)
                }
                Text(
                    text = if (netPnl > 0) "+$formattedAmount" else formattedAmount,
                    color = if (isWin) colors.emeraldWin else if (isLoss) colors.crimsonLoss else colors.textSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )

                // Trade count badge
                Text(
                    text = "$tradeCount ${if (tradeCount == 1) "trd" else "trds"}",
                    color = colors.textMuted,
                    fontSize = 7.5.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CalendarTradeItemCard(
    trade: Trade,
    onClick: () -> Unit,
    onChartClick: () -> Unit
) {
    val colors = LiquidTheme.colors
    val hasChart = trade.screenshotUri.isNotBlank() || trade.exitScreenshotUri.isNotBlank()
    val isWin = trade.result == TradeResult.WIN
    val isLoss = trade.result == TradeResult.LOSS

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        borderColor = if (isWin) colors.emeraldWin.copy(alpha = 0.3f) else if (isLoss) colors.crimsonLoss.copy(alpha = 0.3f) else colors.border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DirectionBadge(direction = trade.direction)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = trade.symbol,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        ResultBadge(result = trade.result)
                    }
                    Text(
                        text = "Entry: $${String.format(Locale.US, "%.2f", trade.entryPrice)}" +
                                if (trade.riskRewardRatio > 0) " • 1:${String.format(Locale.US, "%.1f", trade.riskRewardRatio)} R:R" else "",
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasChart) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.indigoDark.copy(alpha = 0.35f))
                            .border(1.dp, colors.indigoLight.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .clickable(onClick = onChartClick)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "📸 Chart",
                            color = colors.indigoLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                CurrencyPnlText(amount = trade.effectivePnl, fontSize = 14)
            }
        }
    }
}
