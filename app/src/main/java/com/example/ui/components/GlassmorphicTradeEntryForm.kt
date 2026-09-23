package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Trade
import com.example.data.model.TradeDirection
import com.example.data.model.TradeResult
import com.example.ui.theme.LiquidTheme
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

/**
 * High-fidelity, completely opaque Glassmorphic Trade Entry Form.
 *
 * NOTE: The screen behind the entry form is NOT visible (fully opaque background),
 * ensuring zero transparency through to the underlying dashboard or journal,
 * while inside, it features luminous frosted glass cards, vibrant trade side pills,
 * live P&L calculators, and spring physics.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlassmorphicTradeEntryForm(
    trade: Trade? = null,
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (Trade) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidTheme.colors
    val isEditing = trade != null

    // Form States
    var pair by remember { mutableStateOf(trade?.symbol ?: "") }
    var side by remember { mutableStateOf(trade?.direction ?: TradeDirection.LONG) }
    var entryPriceStr by remember {
        mutableStateOf(if (trade != null && trade.entryPrice > 0) trade.entryPrice.toString() else "")
    }
    var exitPriceStr by remember {
        val price = trade?.exitPrice ?: 0.0
        mutableStateOf(if (trade != null && price > 0) price.toString() else "")
    }
    var stopLossStr by remember {
        mutableStateOf(if (trade != null && trade.stopLoss > 0) trade.stopLoss.toString() else "")
    }
    var pnlOverrideStr by remember {
        mutableStateOf(
            if (trade != null && trade.pnl != 0.0) {
                val amt = abs(trade.pnl)
                if (amt % 1.0 == 0.0) amt.toLong().toString() else amt.toString()
            } else ""
        )
    }
    var notes by remember { mutableStateOf(trade?.notes ?: "") }
    var strategy by remember { mutableStateOf(trade?.strategy ?: "") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Validation error triggers
    var showErrorPair by remember { mutableStateOf(false) }
    var showErrorEntry by remember { mutableStateOf(false) }
    var showErrorExit by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            isSubmitting = false
        }
    }

    // Dynamic Calculations
    val entryPrice = entryPriceStr.toDoubleOrNull() ?: 0.0
    val exitPrice = exitPriceStr.toDoubleOrNull() ?: 0.0
    val stopLoss = stopLossStr.toDoubleOrNull() ?: 0.0

    val priceDelta by remember {
        derivedStateOf {
            if (entryPrice > 0 && exitPrice > 0) {
                if (side == TradeDirection.LONG) exitPrice - entryPrice else entryPrice - exitPrice
            } else 0.0
        }
    }

    val returnPercent by remember {
        derivedStateOf {
            if (entryPrice > 0 && exitPrice > 0) {
                (priceDelta / entryPrice) * 100.0
            } else 0.0
        }
    }

    val calculatedResult by remember {
        derivedStateOf {
            if (entryPrice > 0 && exitPrice > 0) {
                when {
                    priceDelta > 0.000001 -> TradeResult.WIN
                    priceDelta < -0.000001 -> TradeResult.LOSS
                    else -> TradeResult.BREAKEVEN
                }
            } else {
                trade?.result ?: TradeResult.WIN
            }
        }
    }

    val calculatedRR by remember {
        derivedStateOf {
            if (entryPrice > 0 && stopLoss > 0 && exitPrice > 0) {
                val risk = abs(entryPrice - stopLoss)
                val reward = abs(exitPrice - entryPrice)
                if (risk > 0.000001) reward / risk else 0.0
            } else 0.0
        }
    }

    val quickPairs = remember {
        listOf("BTC/USDT", "ETH/USDT", "SOL/USDT", "EUR/USD", "GBP/USD", "XAU/USD", "NVDA", "NQ")
    }

    val presetStrategies = remember {
        listOf(
            "Breakout", "Retest", "Trend Following", "Supply & Demand",
            "Support / Resistance", "Price Action", "FVG / SMC",
            "Scalp", "Swing", "Pullback"
        )
    }

    // OPAQUE ROOT CONTAINER: Screen behind the entry form is NOT visible
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg) // 100% Solid Non-Transparent Background
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("glassmorphic_trade_entry_form")
    ) {
        // Decorative subtle ambient glow inside the opaque canvas (does NOT reveal screen behind)
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopEnd)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (side == TradeDirection.LONG) colors.emeraldWin.copy(alpha = 0.12f)
                            else colors.crimsonLoss.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Header Bar
            TradeEntryHeader(
                isEditing = isEditing,
                onDismiss = onDismiss,
                onReset = {
                    pair = ""
                    entryPriceStr = ""
                    exitPriceStr = ""
                    stopLossStr = ""
                    pnlOverrideStr = ""
                    notes = ""
                    strategy = ""
                    showErrorPair = false
                    showErrorEntry = false
                    showErrorExit = false
                }
            )

            // Scrollable Form Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Section 1: Pair & Side Selection
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Section Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ASSET & DIRECTION",
                                color = colors.textSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.indigoAccent.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (pair.isNotEmpty()) pair else "REQUIRED",
                                    color = colors.indigoAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Pair Text Field
                        GlassTextField(
                            value = pair,
                            onValueChange = {
                                pair = it.uppercase()
                                if (showErrorPair && it.isNotBlank()) showErrorPair = false
                            },
                            label = "Trading Pair",
                            placeholder = "e.g. BTC/USDT, ETH/USDT, EUR/USD",
                            isError = showErrorPair,
                            errorMessage = if (showErrorPair) "Please enter or select a trading pair" else null,
                            trailingIcon = {
                                if (pair.isNotEmpty()) {
                                    IconButton(onClick = { pair = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear Pair",
                                            tint = colors.textMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            testTag = "input_pair"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Pair Select Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            quickPairs.forEach { chipPair ->
                                val isSelected = pair.equals(chipPair, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) colors.indigoAccent
                                            else (if (colors.isDark) Color(0x18FFFFFF) else Color(0x10000000))
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) colors.indigoAccent else colors.border,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            pair = chipPair
                                            showErrorPair = false
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("chip_pair_${chipPair.lowercase().replace("/", "_")}")
                                ) {
                                    Text(
                                        text = chipPair,
                                        color = if (isSelected) Color.White else colors.textPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Side Selector (BUY/LONG vs SELL/SHORT)
                        Text(
                            text = "Trade Side",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (colors.isDark) Color(0x1AFFFFFF) else Color(0x140F172A))
                                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // BUY / LONG
                            val isLong = side == TradeDirection.LONG
                            val longBg by animateColorAsState(
                                targetValue = if (isLong) colors.emeraldWin else Color.Transparent,
                                animationSpec = tween(180),
                                label = "long_bg"
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(longBg)
                                    .clickable { side = TradeDirection.LONG }
                                    .testTag("btn_side_long")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = "Long",
                                        tint = if (isLong) Color.White else colors.emeraldWin,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "BUY / LONG",
                                        color = if (isLong) Color.White else colors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // SELL / SHORT
                            val isShort = side == TradeDirection.SHORT
                            val shortBg by animateColorAsState(
                                targetValue = if (isShort) colors.crimsonLoss else Color.Transparent,
                                animationSpec = tween(180),
                                label = "short_bg"
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(shortBg)
                                    .clickable { side = TradeDirection.SHORT }
                                    .testTag("btn_side_short")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = "Short",
                                        tint = if (isShort) Color.White else colors.crimsonLoss,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "SELL / SHORT",
                                        color = if (isShort) Color.White else colors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Execution Prices (Entry & Exit)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "EXECUTION PRICES",
                            color = colors.textSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Entry Price Input
                            GlassTextField(
                                value = entryPriceStr,
                                onValueChange = {
                                    entryPriceStr = it
                                    if (showErrorEntry && it.isNotBlank()) showErrorEntry = false
                                },
                                label = "Entry Price",
                                placeholder = "0.00",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = showErrorEntry,
                                errorMessage = if (showErrorEntry) "Invalid price" else null,
                                modifier = Modifier.weight(1f),
                                testTag = "input_entry_price"
                            )

                            // Exit Price Input
                            GlassTextField(
                                value = exitPriceStr,
                                onValueChange = {
                                    exitPriceStr = it
                                    if (showErrorExit && it.isNotBlank()) showErrorExit = false
                                },
                                label = "Exit Price",
                                placeholder = "0.00",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = showErrorExit,
                                errorMessage = if (showErrorExit) "Invalid price" else null,
                                modifier = Modifier.weight(1f),
                                testTag = "input_exit_price"
                            )
                        }

                        // Live Dynamic P&L / Return Feedback Pill
                        AnimatedVisibility(
                            visible = entryPrice > 0 && exitPrice > 0,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                val isWin = calculatedResult == TradeResult.WIN
                                val isLoss = calculatedResult == TradeResult.LOSS
                                val pillBg = when {
                                    isWin -> colors.emeraldWin.copy(alpha = 0.14f)
                                    isLoss -> colors.crimsonLoss.copy(alpha = 0.14f)
                                    else -> colors.indigoAccent.copy(alpha = 0.14f)
                                }
                                val pillBorder = when {
                                    isWin -> colors.emeraldWin.copy(alpha = 0.4f)
                                    isLoss -> colors.crimsonLoss.copy(alpha = 0.4f)
                                    else -> colors.indigoAccent.copy(alpha = 0.4f)
                                }
                                val pillText = when {
                                    isWin -> colors.emeraldWin
                                    isLoss -> colors.crimsonLoss
                                    else -> colors.textPrimary
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(pillBg)
                                        .border(1.dp, pillBorder, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isWin) Icons.AutoMirrored.Filled.TrendingUp
                                            else Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            tint = pillText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isWin) "PROFITABLE TRADE" else if (isLoss) "LOSS TRADE" else "BREAKEVEN",
                                            color = pillText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "%s%.2f%% (Δ $%,.2f)",
                                            if (returnPercent > 0) "+" else "",
                                            returnPercent,
                                            abs(priceDelta)
                                        ),
                                        color = pillText,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stop Loss (Optional for Risk Management)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassTextField(
                                value = stopLossStr,
                                onValueChange = { stopLossStr = it },
                                label = "Stop Loss (Optional)",
                                placeholder = "0.00",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                testTag = "input_stop_loss"
                            )

                            // Net P&L Override / Amount
                            GlassTextField(
                                value = pnlOverrideStr,
                                onValueChange = { pnlOverrideStr = it },
                                label = "P&L Amount ($)",
                                placeholder = if (abs(priceDelta) > 0) String.format(Locale.US, "%.2f", abs(priceDelta)) else "Auto",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                testTag = "input_pnl_amount"
                            )
                        }

                        if (calculatedRR > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = String.format(Locale.US, "Risk : Reward = 1 : %.2f", calculatedRR),
                                color = colors.indigoAccent,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Strategy & Setup Tags
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "STRATEGY / SETUP",
                            color = colors.textSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            presetStrategies.forEach { preset ->
                                val isSelected = strategy.equals(preset, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) colors.indigoAccent
                                            else (if (colors.isDark) Color(0x18FFFFFF) else Color(0x10000000))
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) colors.indigoAccent else colors.border,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            strategy = if (isSelected) "" else preset
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("strategy_chip_${preset.lowercase().replace(" ", "_")}")
                                ) {
                                    Text(
                                        text = preset,
                                        color = if (isSelected) Color.White else colors.textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        GlassTextField(
                            value = strategy,
                            onValueChange = { strategy = it },
                            label = "Custom Strategy",
                            placeholder = "e.g. 5m Liquidity Sweep, VWAP Cross",
                            testTag = "input_strategy"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 4: Notes & Psychology
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "TRADE REFLECTIONS & NOTES",
                                color = colors.textSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${notes.length} chars",
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Multi-line Notes Input
                        GlassTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = "Notes & Trade Rationale",
                            placeholder = "Log what triggered this trade, execution discipline, rules followed, and emotions...",
                            singleLine = false,
                            minLines = 3,
                            maxLines = 6,
                            testTag = "input_notes"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bottom Sticky Action Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg) // 100% OPAQUE
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(colors.border, Color.Transparent)
                        ),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (colors.isDark) Color(0x18FFFFFF) else Color(0x10000000))
                            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                            .clickable(onClick = onDismiss)
                            .testTag("btn_cancel_trade")
                    ) {
                        Text(
                            text = "Cancel",
                            color = colors.textSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp
                        )
                    }

                    // Save / Log Trade Button
                    val submitButtonText = when {
                        isEditing -> "Update Trade"
                        calculatedResult == TradeResult.WIN -> "Log Win Trade"
                        calculatedResult == TradeResult.LOSS -> "Log Loss Trade"
                        else -> "Log Trade"
                    }
                    val submitGradient = when (calculatedResult) {
                        TradeResult.WIN -> listOf(Color(0xFF047857), Color(0xFF10B981))
                        TradeResult.LOSS -> listOf(Color(0xFFB91C1C), Color(0xFFEF4444))
                        else -> listOf(colors.indigoDark, colors.indigoAccent)
                    }

                    Box(
                        modifier = Modifier.weight(2f)
                    ) {
                        GlassButton(
                            text = submitButtonText,
                            onClick = {
                                if (isSubmitting || isLoading) return@GlassButton

                                // Validate Inputs
                                val validPair = pair.isNotBlank()
                                val validEntry = entryPriceStr.toDoubleOrNull() != null && entryPriceStr.toDouble() > 0
                                val validExit = exitPriceStr.isEmpty() || (exitPriceStr.toDoubleOrNull() != null && exitPriceStr.toDouble() > 0)

                                if (!validPair) showErrorPair = true
                                if (!validEntry) showErrorEntry = true
                                if (!validExit) showErrorExit = true

                                if (!validPair || !validEntry || !validExit) {
                                    return@GlassButton
                                }

                                isSubmitting = true

                                val finalEntry = entryPriceStr.toDouble()
                                val finalExit = exitPriceStr.toDoubleOrNull() ?: 0.0
                                val finalSl = stopLossStr.toDoubleOrNull() ?: 0.0

                                val calculatedAmt = if (finalExit > 0) abs(finalExit - finalEntry) else 0.0
                                val manualAmt = pnlOverrideStr.toDoubleOrNull()
                                val finalRawAmt = manualAmt ?: calculatedAmt

                                val finalPnl = when (calculatedResult) {
                                    TradeResult.LOSS -> -abs(finalRawAmt)
                                    TradeResult.WIN -> abs(finalRawAmt)
                                    TradeResult.BREAKEVEN -> 0.0
                                    TradeResult.OPEN -> finalRawAmt
                                }

                                val finalTrade = Trade(
                                    id = if (!trade?.id.isNullOrBlank()) trade!!.id else UUID.randomUUID().toString(),
                                    userId = trade?.userId ?: "",
                                    symbol = pair.trim(),
                                    direction = side,
                                    entryPrice = finalEntry,
                                    stopLoss = finalSl,
                                    takeProfit = finalExit,
                                    riskRewardRatio = if (calculatedRR > 0) calculatedRR else trade?.riskRewardRatio ?: 0.0,
                                    result = calculatedResult,
                                    pnl = finalPnl,
                                    timestamp = trade?.timestamp ?: System.currentTimeMillis(),
                                    notes = notes.trim(),
                                    strategy = strategy.trim()
                                )

                                onSave(finalTrade)
                            },
                            enabled = !isSubmitting && !isLoading,
                            isLoading = isSubmitting || isLoading,
                            accentGradient = submitGradient,
                            testTag = "btn_save_trade"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TradeEntryHeader(
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    val colors = LiquidTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (colors.isDark) Color(0x18FFFFFF) else Color(0x10000000))
                    .testTag("btn_close_trade_form")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = if (isEditing) "Edit Trade" else "Log New Trade",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Professional Trading Journal Entry",
                    color = colors.textMuted,
                    fontSize = 11.sp
                )
            }
        }

        IconButton(
            onClick = onReset,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (colors.isDark) Color(0x18FFFFFF) else Color(0x10000000))
                .testTag("btn_reset_trade_form")
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset Form",
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
