package com.example.ui.dialogs

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import java.util.TimeZone
import kotlin.math.abs

data class CalendarDayData(
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val dateKey: String,
    val trades: List<Trade>
)

@Composable
fun TradingCalendarDialog(
    trades: List<Trade>,
    onDismiss: () -> Unit
) {
    val colors = LiquidTheme.colors

    val currentCal = remember { Calendar.getInstance(TimeZone.getDefault()) }
    var displayedYear by remember { mutableIntStateOf(currentCal.get(Calendar.YEAR)) }
    var displayedMonth by remember { mutableIntStateOf(currentCal.get(Calendar.MONTH)) }

    var selectedDayKey by remember { mutableStateOf<String?>(null) }

    // Map trades by local "yyyy-MM-dd" date key
    val tradesByDate = remember(trades) {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        trades.groupBy { formatter.format(Date(it.timestamp)) }
    }

    // Days in current month grid
    val daysInGrid = remember(displayedYear, displayedMonth, tradesByDate) {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday...
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val list = mutableListOf<CalendarDayData>()

        // Leading padding days from previous month
        for (i in 1 until firstDayOfWeek) {
            list.add(CalendarDayData(dayNumber = 0, isCurrentMonth = false, dateKey = "", trades = emptyList()))
        }

        val monthFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }

        for (day in 1..maxDays) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val key = monthFormatter.format(cal.time)
            val dayTrades = tradesByDate[key] ?: emptyList()
            list.add(CalendarDayData(dayNumber = day, isCurrentMonth = true, dateKey = key, trades = dayTrades))
        }

        list
    }

    // Month stats summary
    val monthTrades = remember(daysInGrid) {
        daysInGrid.filter { it.isCurrentMonth }.flatMap { it.trades }
    }
    val monthNetPnl = remember(monthTrades) { monthTrades.sumOf { it.effectivePnl } }

    val daysWithTrades = remember(daysInGrid) {
        daysInGrid.filter { it.isCurrentMonth && it.trades.isNotEmpty() }
    }
    val greenDays = remember(daysWithTrades) {
        daysWithTrades.count { day -> day.trades.sumOf { it.effectivePnl } > 0.0001 }
    }
    val redDays = remember(daysWithTrades) {
        daysWithTrades.count { day -> day.trades.sumOf { it.effectivePnl } < -0.0001 }
    }

    val monthName = remember(displayedYear, displayedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
        }
        SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
    }

    val selectedDayTrades = remember(selectedDayKey, tradesByDate) {
        if (selectedDayKey != null) tradesByDate[selectedDayKey] ?: emptyList() else emptyList()
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.94f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cal_scale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(180),
        label = "cal_alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    alpha = animatedAlpha
                }
                .testTag("trading_calendar_dialog"),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 720.dp),
                shape = RoundedCornerShape(26.dp),
                borderColor = colors.indigoAccent.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(colors.indigoAccent.copy(alpha = 0.2f))
                                    .border(1.dp, colors.indigoAccent.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = colors.indigoAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Trading Calendar & Heatmap",
                                    color = colors.textPrimary,
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Daily P&L Performance Grid",
                                    color = colors.textMuted,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (colors.isDark) Color(0x18FFFFFF) else Color(0x10000000))
                                .testTag("btn_close_calendar")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Month Navigation & Summary Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (displayedMonth == 0) {
                                        displayedMonth = 11
                                        displayedYear--
                                    } else {
                                        displayedMonth--
                                    }
                                    selectedDayKey = null
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                            }

                            Text(
                                text = monthName,
                                color = colors.textPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = {
                                    if (displayedMonth == 11) {
                                        displayedMonth = 0
                                        displayedYear++
                                    } else {
                                        displayedMonth++
                                    }
                                    selectedDayKey = null
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Month net PnL badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (monthNetPnl >= 0) colors.emeraldWin.copy(alpha = 0.2f) else colors.crimsonLoss.copy(alpha = 0.2f))
                                .border(1.dp, if (monthNetPnl >= 0) colors.emeraldWin else colors.crimsonLoss, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%s$%,.2f", if (monthNetPnl >= 0) "+" else "-", abs(monthNetPnl)),
                                color = if (monthNetPnl >= 0) colors.emeraldWin else colors.crimsonLoss,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Month Mini Stats
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (colors.isDark) Color(0x14FFFFFF) else Color(0x0C000000))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Green: $greenDays days", color = colors.emeraldWin, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Red: $redDays days", color = colors.crimsonLoss, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Trades: ${monthTrades.size}", color = colors.indigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Weekday Headers
                    val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        weekdays.forEach { dayName ->
                            Text(
                                text = dayName,
                                color = colors.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Calendar Grid & Selected Day Inspector
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 7-Column Heatmap Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            userScrollEnabled = false
                        ) {
                            items(daysInGrid) { dayData ->
                                if (!dayData.isCurrentMonth || dayData.dayNumber == 0) {
                                    Box(modifier = Modifier.aspectRatio(1f))
                                } else {
                                    val isSelected = dayData.dateKey == selectedDayKey
                                    val dayTrades = dayData.trades
                                    val dayNetPnl = dayTrades.sumOf { it.effectivePnl }
                                    val hasTrades = dayTrades.isNotEmpty()

                                    val tileBg = when {
                                        !hasTrades -> if (colors.isDark) Color(0x10FFFFFF) else Color(0x08000000)
                                        dayNetPnl > 0.0001 -> colors.emeraldWin.copy(alpha = if (isSelected) 0.5f else 0.28f)
                                        dayNetPnl < -0.0001 -> colors.crimsonLoss.copy(alpha = if (isSelected) 0.5f else 0.28f)
                                        else -> colors.indigoAccent.copy(alpha = if (isSelected) 0.5f else 0.22f)
                                    }

                                    val tileBorder = when {
                                        isSelected -> colors.indigoAccent
                                        !hasTrades -> colors.border.copy(alpha = 0.4f)
                                        dayNetPnl > 0.0001 -> colors.emeraldWin.copy(alpha = 0.8f)
                                        dayNetPnl < -0.0001 -> colors.crimsonLoss.copy(alpha = 0.8f)
                                        else -> colors.border
                                    }

                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(tileBg)
                                            .border(if (isSelected) 1.5.dp else 1.dp, tileBorder, RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedDayKey = if (isSelected) null else dayData.dateKey
                                            }
                                            .padding(3.dp),
                                        contentAlignment = Alignment.TopStart
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = dayData.dayNumber.toString(),
                                                color = if (hasTrades) Color.White else colors.textMuted,
                                                fontSize = 10.sp,
                                                fontWeight = if (hasTrades) FontWeight.Bold else FontWeight.Normal
                                            )

                                            if (hasTrades) {
                                                val pnlText = if (dayNetPnl >= 0) "+$${dayNetPnl.toInt()}" else "-$${abs(dayNetPnl).toInt()}"
                                                Text(
                                                    text = pnlText,
                                                    color = if (dayNetPnl >= 0) colors.emeraldWin else colors.crimsonLoss,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Selected Day Trades Inspector
                        if (selectedDayKey != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "TRADES ON $selectedDayKey (${selectedDayTrades.size})",
                                color = colors.textSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (selectedDayTrades.isEmpty()) {
                                Text(
                                    text = "No trades recorded on this date.",
                                    color = colors.textMuted,
                                    fontSize = 12.sp
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    selectedDayTrades.forEach { trade ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (colors.isDark) Color(0x14FFFFFF) else Color(0x0A000000))
                                                .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                DirectionBadge(direction = trade.direction)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text = trade.symbol, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                ResultBadge(result = trade.result)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                CurrencyPnlText(amount = trade.effectivePnl, fontSize = 13)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
