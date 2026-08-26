package com.example.ui.dialogs

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Trade
import com.example.data.model.TradeDirection
import com.example.data.model.TradeResult
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassTextField
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.CrimsonLossBg
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.EmeraldWinBg
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.IndigoDark
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrophyGold
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTradeDialog(
    trade: Trade?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (Trade) -> Unit
) {
    val isEditing = trade != null

    var symbol by remember { mutableStateOf(trade?.symbol ?: "") }
    var direction by remember { mutableStateOf(trade?.direction ?: TradeDirection.LONG) }
    var entryPriceStr by remember { mutableStateOf(if (trade != null && trade.entryPrice > 0) trade.entryPrice.toString() else "") }
    var stopLossStr by remember { mutableStateOf(if (trade != null && trade.stopLoss > 0) trade.stopLoss.toString() else "") }
    var takeProfitStr by remember { mutableStateOf(if (trade != null && trade.takeProfit > 0) trade.takeProfit.toString() else "") }
    var pnlStr by remember { mutableStateOf(if (trade != null) trade.pnl.toString() else "") }
    var result by remember { mutableStateOf(trade?.result ?: TradeResult.WIN) }
    var strategy by remember { mutableStateOf(trade?.strategy ?: "") }
    var notes by remember { mutableStateOf(trade?.notes ?: "") }

    // Live calculated Risk:Reward ratio
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

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("dialog_add_edit_trade")
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
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEditing) "EDIT TRADE" else "LOG NEW TRADE",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Record entry, targets, and outcome",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
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

                Spacer(modifier = Modifier.height(16.dp))

                // Asset / Symbol
                GlassTextField(
                    value = symbol,
                    onValueChange = { symbol = it.uppercase() },
                    label = "Asset / Pair Symbol",
                    placeholder = "e.g. BTC/USDT, EUR/USD, NVDA, SPY",
                    testTag = "input_trade_symbol"
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Direction Selector (LONG vs SHORT)
                Text("Position Direction", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0DFFFFFF))
                        .border(1.dp, SleekBorder, RoundedCornerShape(12.dp))
                        .padding(3.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (direction == TradeDirection.LONG) EmeraldWinBg else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (direction == TradeDirection.LONG) EmeraldWin else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { direction = TradeDirection.LONG }
                            .testTag("direction_long")
                    ) {
                        Text(
                            "LONG ▲",
                            color = if (direction == TradeDirection.LONG) EmeraldWin else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (direction == TradeDirection.SHORT) CrimsonLossBg else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (direction == TradeDirection.SHORT) CrimsonLoss else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { direction = TradeDirection.SHORT }
                            .testTag("direction_short")
                    ) {
                        Text(
                            "SHORT ▼",
                            color = if (direction == TradeDirection.SHORT) CrimsonLoss else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Price Inputs (Entry, SL, TP)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassTextField(
                        value = entryPriceStr,
                        onValueChange = { entryPriceStr = it },
                        label = "Entry Price",
                        placeholder = "0.00",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        testTag = "input_entry_price"
                    )

                    GlassTextField(
                        value = stopLossStr,
                        onValueChange = { stopLossStr = it },
                        label = "Stop Loss",
                        placeholder = "0.00",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        testTag = "input_stop_loss"
                    )

                    GlassTextField(
                        value = takeProfitStr,
                        onValueChange = { takeProfitStr = it },
                        label = "Take Profit",
                        placeholder = "0.00",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        testTag = "input_take_profit"
                    )
                }

                // Live R:R feedback badge
                if (calculatedRR > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(IndigoDark.copy(alpha = 0.4f))
                            .border(1.dp, IndigoLight.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = IndigoLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format(Locale.US, "Calculated R:R Ratio = 1 : %.2f", calculatedRR),
                            color = IndigoLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Result Selector (WIN / LOSS / BREAKEVEN)
                Text("Trade Outcome", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(TradeResult.WIN, TradeResult.LOSS, TradeResult.BREAKEVEN).forEach { res ->
                        val isSelected = result == res
                        val color = when (res) {
                            TradeResult.WIN -> EmeraldWin
                            TradeResult.LOSS -> CrimsonLoss
                            TradeResult.BREAKEVEN -> IndigoLight
                            TradeResult.OPEN -> TrophyGold
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) color.copy(alpha = 0.2f) else Color(0x0DFFFFFF))
                                .border(
                                    1.dp,
                                    if (isSelected) color else SleekBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    result = res
                                    if (res == TradeResult.BREAKEVEN && pnlStr.isBlank()) {
                                        pnlStr = "0.0"
                                    }
                                }
                                .testTag("result_${res.name.lowercase()}")
                        ) {
                            Text(
                                res.name,
                                color = if (isSelected) color else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Realized P&L ($)
                GlassTextField(
                    value = pnlStr,
                    onValueChange = { pnlStr = it },
                    label = "Realized P&L ($)",
                    placeholder = if (result == TradeResult.WIN) "+450.00" else "-150.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    testTag = "input_pnl"
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Strategy Tag
                GlassTextField(
                    value = strategy,
                    onValueChange = { strategy = it },
                    label = "Setup / Strategy Tag",
                    placeholder = "e.g. Liquidity Sweep, Fair Value Gap, Breakout",
                    testTag = "input_strategy"
                )

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

                // Save Action Button
                GlassButton(
                    text = if (isEditing) "UPDATE TRADE" else "RECORD TRADE",
                    onClick = {
                        val entry = entryPriceStr.toDoubleOrNull() ?: 0.0
                        val sl = stopLossStr.toDoubleOrNull() ?: 0.0
                        val tp = takeProfitStr.toDoubleOrNull() ?: 0.0
                        val pnl = pnlStr.toDoubleOrNull() ?: 0.0
                        val finalRR = if (calculatedRR > 0) calculatedRR else trade?.riskRewardRatio ?: 0.0

                        val finalTrade = Trade(
                            id = trade?.id ?: "",
                            userId = trade?.userId ?: "",
                            symbol = symbol.trim().ifEmpty { "UNKNOWN" },
                            direction = direction,
                            entryPrice = entry,
                            stopLoss = sl,
                            takeProfit = tp,
                            riskRewardRatio = finalRR,
                            result = result,
                            pnl = pnl,
                            timestamp = trade?.timestamp ?: System.currentTimeMillis(),
                            notes = notes.trim(),
                            strategy = strategy.trim()
                        )
                        onSave(finalTrade)
                    },
                    isLoading = isLoading,
                    accentGradient = listOf(IndigoDark, IndigoAccent),
                    testTag = "btn_save_trade"
                )
            }
        }
    }
}

