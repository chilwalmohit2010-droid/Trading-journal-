package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Trade
import com.example.data.model.TradeDirection
import com.example.data.model.TradeResult
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassTextField
import com.example.ui.theme.LiquidTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditTradeDialog(
    trade: Trade?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (Trade) -> Unit
) {
    val colors = LiquidTheme.colors
    val isEditing = trade != null

    var symbol by remember { mutableStateOf(trade?.symbol ?: "") }
    var direction by remember { mutableStateOf(trade?.direction ?: TradeDirection.LONG) }
    var entryPriceStr by remember { mutableStateOf(if (trade != null && trade.entryPrice > 0) trade.entryPrice.toString() else "") }
    var stopLossStr by remember { mutableStateOf(if (trade != null && trade.stopLoss > 0) trade.stopLoss.toString() else "") }
    var takeProfitStr by remember { mutableStateOf(if (trade != null && trade.takeProfit > 0) trade.takeProfit.toString() else "") }
    var pnlStr by remember {
        mutableStateOf(
            if (trade != null) {
                val amt = abs(trade.pnl)
                if (amt == 0.0) "" else if (amt % 1.0 == 0.0) amt.toLong().toString() else amt.toString()
            } else ""
        )
    }
    var result by remember { mutableStateOf(trade?.result ?: TradeResult.WIN) }
    var strategy by remember { mutableStateOf(trade?.strategy ?: "") }
    var notes by remember { mutableStateOf(trade?.notes ?: "") }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            isSubmitting = false
        }
    }

    val calculatedRR by remember {
        derivedStateOf {
            val entry = entryPriceStr.toDoubleOrNull() ?: 0.0
            val sl = stopLossStr.toDoubleOrNull() ?: 0.0
            val tp = takeProfitStr.toDoubleOrNull() ?: 0.0
            if (entry > 0 && sl > 0 && tp > 0) {
                val risk = abs(entry - sl)
                val reward = abs(tp - entry)
                if (risk > 0.000001) reward / risk else 0.0
            } else {
                0.0
            }
        }
    }

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
                        onClick = { /* Consume touch events inside dialog */ }
                    )
                    .testTag("dialog_add_edit_trade"),
                shape = RoundedCornerShape(24.dp)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // Dialog Title Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Trade Entry" else "Record Trade",
                        color = colors.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close dialog",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Symbol / Instrument
                GlassTextField(
                    value = symbol,
                    onValueChange = { symbol = it.uppercase() },
                    label = "Asset / Symbol",
                    placeholder = "e.g. BTCUSDT, EURUSD, NVDA, NQ",
                    testTag = "input_symbol"
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Direction: LONG vs SHORT
                Text(
                    text = "Trade Direction",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (colors.isDark) Color(0x14FFFFFF) else Color(0x140F172A))
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(3.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (direction == TradeDirection.LONG) colors.emeraldWin else Color.Transparent)
                            .clickable { direction = TradeDirection.LONG }
                            .testTag("btn_select_long")
                    ) {
                        Text(
                            text = "LONG ▲",
                            color = if (direction == TradeDirection.LONG) Color.White else colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (direction == TradeDirection.SHORT) colors.crimsonLoss else Color.Transparent)
                            .clickable { direction = TradeDirection.SHORT }
                            .testTag("btn_select_short")
                    ) {
                        Text(
                            text = "SHORT ▼",
                            color = if (direction == TradeDirection.SHORT) Color.White else colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Outcome: WIN vs LOSS vs BE
                Text(
                    text = "Trade Outcome",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (colors.isDark) Color(0x14FFFFFF) else Color(0x140F172A))
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(3.dp)
                ) {
                    val winBg by animateColorAsState(
                        targetValue = if (result == TradeResult.WIN) colors.emeraldWin else Color.Transparent,
                        animationSpec = tween(220),
                        label = "win_bg"
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(winBg)
                            .clickable { result = TradeResult.WIN }
                            .testTag("btn_select_win")
                    ) {
                        Text(
                            text = "WIN",
                            color = if (result == TradeResult.WIN) Color.White else colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    val lossBg by animateColorAsState(
                        targetValue = if (result == TradeResult.LOSS) colors.crimsonLoss else Color.Transparent,
                        animationSpec = tween(220),
                        label = "loss_bg"
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(lossBg)
                            .clickable { result = TradeResult.LOSS }
                            .testTag("btn_select_loss")
                    ) {
                        Text(
                            text = "LOSS",
                            color = if (result == TradeResult.LOSS) Color.White else colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    val beBg by animateColorAsState(
                        targetValue = if (result == TradeResult.BREAKEVEN) colors.textMuted else Color.Transparent,
                        animationSpec = tween(220),
                        label = "be_bg"
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(beBg)
                            .clickable { result = TradeResult.BREAKEVEN }
                            .testTag("btn_select_be")
                    ) {
                        Text(
                            text = "B/E",
                            color = if (result == TradeResult.BREAKEVEN) Color.White else colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Price Levels Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassTextField(
                        value = entryPriceStr,
                        onValueChange = { entryPriceStr = it },
                        label = "Entry Price",
                        placeholder = "0.00",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        testTag = "input_entry_price"
                    )

                    GlassTextField(
                        value = stopLossStr,
                        onValueChange = { stopLossStr = it },
                        label = "Stop Loss",
                        placeholder = "0.00",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        testTag = "input_stop_loss"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassTextField(
                        value = takeProfitStr,
                        onValueChange = { takeProfitStr = it },
                        label = "Take Profit",
                        placeholder = "0.00",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        testTag = "input_take_profit"
                    )

                    // Calculated R:R Display Pill
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Risk:Reward (Auto)",
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (colors.isDark) Color(0x14FFFFFF) else Color(0x140F172A))
                                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                text = if (calculatedRR > 0) String.format(Locale.US, "1 : %.2f", calculatedRR) else "—",
                                color = if (calculatedRR >= 2.0) colors.trophyGold else colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Realized P&L ($) with loss feedback and colored indicator
                val pnlLabel = when (result) {
                    TradeResult.LOSS -> "Realized Loss Amount ($)"
                    TradeResult.WIN -> "Realized Profit Amount ($)"
                    else -> "Realized P&L ($)"
                }
                val pnlPlaceholder = when (result) {
                    TradeResult.LOSS -> "e.g. 150.00 (deducted as loss)"
                    TradeResult.WIN -> "e.g. 250.00 (added as profit)"
                    else -> "0.00"
                }

                Column {
                    GlassTextField(
                        value = pnlStr,
                        onValueChange = { pnlStr = it },
                        label = pnlLabel,
                        placeholder = pnlPlaceholder,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        testTag = "input_pnl"
                    )

                    AnimatedVisibility(
                        visible = result == TradeResult.LOSS && pnlStr.isNotBlank(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val parsedAmt = pnlStr.toDoubleOrNull()
                        val lossDisplay = if (parsedAmt != null) String.format(Locale.US, "-$%,.2f", abs(parsedAmt)) else "-$0.00"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, start = 4.dp, end = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.crimsonLoss.copy(alpha = 0.12f))
                                .border(1.dp, colors.crimsonLoss.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = colors.crimsonLoss,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Losing Trade: $lossDisplay will be deducted from your Net P&L",
                                color = colors.crimsonLoss,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = result == TradeResult.WIN && pnlStr.isNotBlank(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val parsedAmt = pnlStr.toDoubleOrNull()
                        val winDisplay = if (parsedAmt != null) String.format(Locale.US, "+$%,.2f", abs(parsedAmt)) else "+$0.00"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, start = 4.dp, end = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.emeraldWin.copy(alpha = 0.12f))
                                .border(1.dp, colors.emeraldWin.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = colors.emeraldWin,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Winning Trade: $winDisplay will be added to your Net P&L",
                                color = colors.emeraldWin,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Strategy Selection Chips (Requirement 8)
                Text(
                    text = "Trading Strategy / Setup",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                val presetStrategies = remember {
                    listOf(
                        "Breakout",
                        "Retest",
                        "Trend Following",
                        "Support & Resistance",
                        "Supply & Demand",
                        "Price Action",
                        "Pullback",
                        "Reversal",
                        "Range Trading",
                        "Liquidity Sweep",
                        "Fair Value Gap (FVG)",
                        "Order Block",
                        "Smart Money (SMC)",
                        "ICT",
                        "VWAP Bounce",
                        "EMA Crossover",
                        "RSI Divergence",
                        "Scalping",
                        "Swing Trading",
                        "Other"
                    )
                }

                var isCustomStrategy by remember {
                    mutableStateOf(strategy.isNotEmpty() && !presetStrategies.filter { it != "Other" }.contains(strategy))
                }

                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetStrategies.forEach { preset ->
                        val isSelected = if (preset == "Other") {
                            isCustomStrategy
                        } else {
                            !isCustomStrategy && strategy.equals(preset, ignoreCase = true)
                        }

                        val chipBg = if (isSelected) colors.indigoAccent else (if (colors.isDark) Color(0x14FFFFFF) else Color(0x0F0F172A))
                        val chipBorder = if (isSelected) colors.indigoAccent else colors.border
                        val chipTextColor = if (isSelected) Color.White else colors.textPrimary

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(chipBg)
                                .border(1.dp, chipBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    if (preset == "Other") {
                                        isCustomStrategy = true
                                        if (presetStrategies.contains(strategy)) {
                                            strategy = ""
                                        }
                                    } else {
                                        isCustomStrategy = false
                                        strategy = preset
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                .testTag("strategy_chip_${preset.lowercase().replace(" ", "_")}")
                        ) {
                            Text(
                                text = preset,
                                color = chipTextColor,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                if (isCustomStrategy) {
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassTextField(
                        value = strategy,
                        onValueChange = { strategy = it },
                        label = "Custom Strategy Name",
                        placeholder = "e.g. Fib Golden Pocket, Morning Star",
                        testTag = "input_custom_strategy"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notes
                GlassTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Trade Notes & Psychology",
                    placeholder = "Execution details, mindset, exit reasoning...",
                    testTag = "input_notes"
                )

                Spacer(modifier = Modifier.height(24.dp))

                val buttonText = when {
                    isEditing -> "UPDATE TRADE"
                    result == TradeResult.LOSS -> "RECORD LOSS TRADE"
                    result == TradeResult.WIN -> "RECORD WIN TRADE"
                    else -> "RECORD TRADE"
                }
                val buttonGradient = when (result) {
                    TradeResult.LOSS -> listOf(Color(0xFF991B1B), Color(0xFFDC2626))
                    TradeResult.WIN -> listOf(Color(0xFF065F46), Color(0xFF10B981))
                    else -> listOf(colors.indigoDark, colors.indigoAccent)
                }

                // Save Action Button
                GlassButton(
                    text = buttonText,
                    onClick = {
                        if (isSubmitting || isLoading) return@GlassButton
                        isSubmitting = true

                        val entry = entryPriceStr.toDoubleOrNull() ?: 0.0
                        val sl = stopLossStr.toDoubleOrNull() ?: 0.0
                        val tp = takeProfitStr.toDoubleOrNull() ?: 0.0
                        val rawPnl = pnlStr.toDoubleOrNull() ?: 0.0
                        val finalPnl = when (result) {
                            TradeResult.LOSS -> -abs(rawPnl)
                            TradeResult.WIN -> abs(rawPnl)
                            TradeResult.BREAKEVEN -> 0.0
                            TradeResult.OPEN -> rawPnl
                        }
                        val finalRR = if (calculatedRR > 0) calculatedRR else trade?.riskRewardRatio ?: 0.0

                        val tradeId = if (!trade?.id.isNullOrBlank()) trade!!.id else java.util.UUID.randomUUID().toString()
                        val finalTrade = Trade(
                            id = tradeId,
                            userId = trade?.userId ?: "",
                            symbol = symbol.trim().ifEmpty { "UNKNOWN" },
                            direction = direction,
                            entryPrice = entry,
                            stopLoss = sl,
                            takeProfit = tp,
                            riskRewardRatio = finalRR,
                            result = result,
                            pnl = finalPnl,
                            timestamp = trade?.timestamp ?: System.currentTimeMillis(),
                            notes = notes.trim(),
                            strategy = strategy.trim()
                        )
                        onSave(finalTrade)
                    },
                    enabled = !isSubmitting && !isLoading,
                    isLoading = isSubmitting || isLoading,
                    accentGradient = buttonGradient,
                    testTag = "btn_save_trade"
                )
            }
        }
    }
}
}
