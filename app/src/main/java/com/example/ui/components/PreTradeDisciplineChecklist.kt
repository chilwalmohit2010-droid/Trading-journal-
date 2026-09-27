package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LiquidTheme

private const val PREFS_NAME = "trading_discipline_prefs"
private const val KEY_CUSTOM_RULES = "saved_rules_json"

private val DEFAULT_DISCIPLINE_RULES = listOf(
    "Aligned with Higher Timeframe (4H / Daily) trend?",
    "Risk strictly defined & <= 2% account equity?",
    "Key Support / Resistance or Liquidity level retested?",
    "Pre-defined Stop Loss and Take Profit established?",
    "Psychology Check: No FOMO, revenge trading, or boredom?",
    "Calculated Risk-to-Reward ratio is >= 1 : 2.0?"
)

/**
 * Pre-Trade Discipline Checklist component.
 * Enforces trading psychology, confirms technical alignment,
 * and prevents impulsive FOMO / revenge trading.
 */
@Composable
fun PreTradeDisciplineChecklist(
    onScoreChanged: (scorePercent: Int, passedRules: Int, totalRules: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LiquidTheme.colors

    // Load saved or default rules
    val sharedPrefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val rulesList = remember {
        val savedSet = sharedPrefs.getStringSet(KEY_CUSTOM_RULES, null)
        val initialList = if (!savedSet.isNullOrEmpty()) savedSet.toList() else DEFAULT_DISCIPLINE_RULES
        mutableStateListOf<String>().apply { addAll(initialList) }
    }

    // State of checked rules: Rule string to Boolean
    val checkedMap = remember {
        mutableStateListOf<String>()
    }

    var isAddingRule by remember { mutableStateOf(false) }
    var newRuleText by remember { mutableStateOf("") }

    // Calculate score
    val totalRules = rulesList.size
    val passedRules = checkedMap.size
    val scorePercent = if (totalRules > 0) (passedRules * 100) / totalRules else 100

    LaunchedEffect(passedRules, totalRules) {
        onScoreChanged(scorePercent, passedRules, totalRules)
    }

    fun saveRulesToPrefs() {
        sharedPrefs.edit().putStringSet(KEY_CUSTOM_RULES, rulesList.toSet()).apply()
    }

    val statusColor = when {
        scorePercent >= 100 -> colors.emeraldWin
        scorePercent >= 66 -> colors.indigoLight
        scorePercent >= 40 -> Color(0xFFF59E0B) // Amber
        else -> colors.crimsonLoss
    }

    val statusLabel = when {
        scorePercent >= 100 -> "100% Rules Passed • Pristine Execution"
        scorePercent >= 66 -> "$passedRules/$totalRules Rules Passed • High Probability"
        scorePercent >= 40 -> "Caution: $passedRules/$totalRules Rules • Check Violated Rules"
        else -> "⚠️ High Risk: $passedRules/$totalRules Rules • Avoid Revenge / FOMO"
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pre_trade_discipline_checklist"),
        shape = RoundedCornerShape(18.dp),
        borderColor = statusColor.copy(alpha = 0.4f),
        glowColor = if (scorePercent < 50) colors.crimsonLoss.copy(alpha = 0.12f) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title, Icon, and Score Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f))
                            .border(1.dp, statusColor.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (scorePercent >= 66) Icons.Default.Rule else Icons.Default.Psychology,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PRE-TRADE DISCIPLINE",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Psychology & Execution Gate",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Score Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusColor.copy(alpha = 0.18f))
                        .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$scorePercent%",
                        color = statusColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status message
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                if (scorePercent < 50) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = statusLabel,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Checklist Items
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rulesList.forEachIndexed { index, ruleText ->
                    val isChecked = checkedMap.contains(ruleText)
                    DisciplineCheckItem(
                        text = ruleText,
                        isChecked = isChecked,
                        onToggle = {
                            if (isChecked) {
                                checkedMap.remove(ruleText)
                            } else {
                                checkedMap.add(ruleText)
                            }
                        },
                        onDelete = if (rulesList.size > 2) {
                            {
                                rulesList.removeAt(index)
                                checkedMap.remove(ruleText)
                                saveRulesToPrefs()
                            }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Actions: Add Rule & Quick "Check All"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add Custom Rule Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { isAddingRule = true }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("btn_add_discipline_rule")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Rule",
                        tint = colors.indigoLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Custom Rule",
                        color = colors.indigoLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Check All / Clear Toggle
                Text(
                    text = if (passedRules == totalRules) "Clear All" else "Pass All Rules",
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable {
                            if (passedRules == totalRules) {
                                checkedMap.clear()
                            } else {
                                checkedMap.clear()
                                checkedMap.addAll(rulesList)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }

    // Add Rule Dialog
    if (isAddingRule) {
        AlertDialog(
            onDismissRequest = { isAddingRule = false },
            title = {
                Text(
                    text = "Add Discipline Rule",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Define a personal rule or condition that must be verified before taking any setup.",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newRuleText,
                        onValueChange = { newRuleText = it },
                        placeholder = { Text("e.g. Waited for 15m candle close?") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.indigoAccent,
                            unfocusedBorderColor = colors.border
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newRuleText.trim()
                        if (trimmed.isNotBlank()) {
                            rulesList.add(trimmed)
                            checkedMap.add(trimmed)
                            saveRulesToPrefs()
                            newRuleText = ""
                            isAddingRule = false
                        }
                    }
                ) {
                    Text("Add Rule", color = colors.indigoAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddingRule = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun DisciplineCheckItem(
    text: String,
    isChecked: Boolean,
    onToggle: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val colors = LiquidTheme.colors

    val checkboxBg by animateColorAsState(
        targetValue = if (isChecked) colors.emeraldWin else Color.Transparent,
        animationSpec = tween(150),
        label = "box_bg"
    )
    val checkboxBorder by animateColorAsState(
        targetValue = if (isChecked) colors.emeraldWin else colors.border,
        animationSpec = tween(150),
        label = "box_border"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isChecked) colors.emeraldWin.copy(alpha = 0.07f) else if (colors.isDark) Color(0x0AFFFFFF) else Color(0x06000000))
            .border(
                1.dp,
                if (isChecked) colors.emeraldWin.copy(alpha = 0.35f) else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Custom animated Checkbox
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(checkboxBg)
                .border(1.5.dp, checkboxBorder, RoundedCornerShape(6.dp))
        ) {
            if (isChecked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            color = if (isChecked) colors.textPrimary else colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Rule",
                    tint = colors.textMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
