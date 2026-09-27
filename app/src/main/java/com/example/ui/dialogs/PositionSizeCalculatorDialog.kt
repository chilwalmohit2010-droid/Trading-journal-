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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassTextField
import com.example.ui.theme.LiquidTheme
import java.util.Locale
import kotlin.math.abs

enum class AssetClass(val label: String, val unitLabel: String) {
    CRYPTO_STOCKS("Crypto / Stocks", "Units / Coins"),
    FOREX("Forex (100k Lots)", "Standard Lots"),
    INDICES_FUTURES("Indices / Futures", "Contracts")
}

@Composable
fun PositionSizeCalculatorDialog(
    initialEntryPrice: Double = 0.0,
    initialStopLoss: Double = 0.0,
    onDismiss: () -> Unit,
    onApplyToTrade: ((entry: Double, stopLoss: Double, positionSize: Double) -> Unit)? = null
) {
    val colors = LiquidTheme.colors

    var accountBalanceStr by remember { mutableStateOf("10000") }
    var riskPercentStr by remember { mutableStateOf("1.5") }
    var entryPriceStr by remember { mutableStateOf(if (initialEntryPrice > 0) initialEntryPrice.toString() else "64000") }
    var stopLossPriceStr by remember { mutableStateOf(if (initialStopLoss > 0) initialStopLoss.toString() else "63200") }
    var selectedAssetClass by remember { mutableStateOf(AssetClass.CRYPTO_STOCKS) }

    val accountBalance = accountBalanceStr.toDoubleOrNull() ?: 0.0
    val riskPercent = riskPercentStr.toDoubleOrNull() ?: 0.0
    val entryPrice = entryPriceStr.toDoubleOrNull() ?: 0.0
    val stopLossPrice = stopLossPriceStr.toDoubleOrNull() ?: 0.0

    // Calculations
    val dollarRisk by remember {
        derivedStateOf {
            if (accountBalance > 0 && riskPercent > 0) accountBalance * (riskPercent / 100.0) else 0.0
        }
    }

    val slDistance by remember {
        derivedStateOf {
            if (entryPrice > 0 && stopLossPrice > 0) abs(entryPrice - stopLossPrice) else 0.0
        }
    }

    val slPercent by remember {
        derivedStateOf {
            if (entryPrice > 0 && slDistance > 0) (slDistance / entryPrice) * 100.0 else 0.0
        }
    }

    // Units / Shares / Coins
    val unitsSize by remember {
        derivedStateOf {
            if (slDistance > 0.000001 && dollarRisk > 0) dollarRisk / slDistance else 0.0
        }
    }

    // Forex Standard Lots (100,000 units = 1 lot, assuming 1 pip = 0.0001)
    val forexLots by remember {
        derivedStateOf {
            if (entryPrice > 0 && slDistance > 0.000001 && dollarRisk > 0) {
                // Approximate standard lot calculation
                val pips = slDistance * 10000.0
                if (pips > 0) dollarRisk / (pips * 10.0) else unitsSize / 100000.0
            } else 0.0
        }
    }

    val totalPositionValue by remember {
        derivedStateOf {
            if (entryPrice > 0 && unitsSize > 0) unitsSize * entryPrice else 0.0
        }
    }

    val balancePresets = remember { listOf("1000", "5000", "10000", "25000", "50000", "100000") }
    val riskPresets = remember { listOf("0.5", "1.0", "1.5", "2.0", "3.0") }

    // Dialog animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.94f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "calc_scale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(180),
        label = "calc_alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    alpha = animatedAlpha
                }
                .testTag("position_size_calculator_dialog"),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 700.dp),
                shape = RoundedCornerShape(26.dp),
                borderColor = colors.indigoAccent.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
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
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = colors.indigoAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Position Size Calculator",
                                    color = colors.textPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Risk Management & Lot Sizing",
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
                                .testTag("btn_close_calculator")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scrollable Inputs & Results
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Asset Class Switcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (colors.isDark) Color(0x1AFFFFFF) else Color(0x10000000))
                                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AssetClass.values().forEach { assetClass ->
                                val isSelected = assetClass == selectedAssetClass
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) colors.indigoAccent else Color.Transparent)
                                        .clickable { selectedAssetClass = assetClass }
                                        .padding(vertical = 7.dp)
                                ) {
                                    Text(
                                        text = if (assetClass == AssetClass.CRYPTO_STOCKS) "Crypto/Stocks" else if (assetClass == AssetClass.FOREX) "Forex Lots" else "Futures",
                                        color = if (isSelected) Color.White else colors.textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Account Balance Input
                        GlassTextField(
                            value = accountBalanceStr,
                            onValueChange = { accountBalanceStr = it },
                            label = "Account Balance ($)",
                            placeholder = "10000.00",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Paid, contentDescription = null, tint = colors.emeraldWin, modifier = Modifier.size(18.dp))
                            },
                            testTag = "input_calc_balance"
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick Balance Presets
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            balancePresets.forEach { preset ->
                                val isSelected = accountBalanceStr == preset
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) colors.indigoAccent else Color(0x12FFFFFF))
                                        .clickable { accountBalanceStr = preset }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$${preset.take(preset.length - 3)}k",
                                        color = if (isSelected) Color.White else colors.textMuted,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Risk % Input
                        GlassTextField(
                            value = riskPercentStr,
                            onValueChange = { riskPercentStr = it },
                            label = "Risk Percentage (%)",
                            placeholder = "1.5",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.ElectricBolt, contentDescription = null, tint = colors.trophyGold, modifier = Modifier.size(18.dp))
                            },
                            testTag = "input_calc_risk_percent"
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick Risk Presets
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            riskPresets.forEach { preset ->
                                val isSelected = riskPercentStr == preset
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) colors.indigoAccent else Color(0x12FFFFFF))
                                        .clickable { riskPercentStr = preset }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$preset%",
                                        color = if (isSelected) Color.White else colors.textMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Entry Price & Stop Loss
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassTextField(
                                value = entryPriceStr,
                                onValueChange = { entryPriceStr = it },
                                label = "Entry Price",
                                placeholder = "0.00",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                testTag = "input_calc_entry"
                            )
                            GlassTextField(
                                value = stopLossPriceStr,
                                onValueChange = { stopLossPriceStr = it },
                                label = "Stop Loss",
                                placeholder = "0.00",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                testTag = "input_calc_sl"
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // RESULTS CARD
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.5.dp, colors.emeraldWin.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CALCULATED POSITION SIZE",
                                        color = colors.textMuted,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Risk: $${String.format(Locale.US, "%.2f", dollarRisk)}",
                                        color = colors.crimsonLoss,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Main result number
                                val primarySizeText = when (selectedAssetClass) {
                                    AssetClass.FOREX -> String.format(Locale.US, "%.2f Lots", forexLots)
                                    AssetClass.CRYPTO_STOCKS -> String.format(Locale.US, "%,.4f Units", unitsSize)
                                    AssetClass.INDICES_FUTURES -> String.format(Locale.US, "%,.2f Contracts", unitsSize)
                                }

                                Text(
                                    text = primarySizeText,
                                    color = Color(0xFF67E8F9),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = "SL Distance", color = colors.textMuted, fontSize = 10.sp)
                                        Text(
                                            text = String.format(Locale.US, "$%,.2f (%.2f%%)", slDistance, slPercent),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "Position Value", color = colors.textMuted, fontSize = 10.sp)
                                        Text(
                                            text = String.format(Locale.US, "$%,.2f", totalPositionValue),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Apply Button
                    if (onApplyToTrade != null) {
                        GlassButton(
                            text = "Apply Size to Trade Form",
                            onClick = {
                                onApplyToTrade(entryPrice, stopLossPrice, unitsSize)
                                onDismiss()
                            },
                            accentGradient = listOf(colors.indigoDark, colors.indigoAccent),
                            testTag = "btn_apply_calc_size"
                        )
                    } else {
                        GlassButton(
                            text = "Close Calculator",
                            onClick = onDismiss,
                            testTag = "btn_dismiss_calc"
                        )
                    }
                }
            }
        }
    }
}
