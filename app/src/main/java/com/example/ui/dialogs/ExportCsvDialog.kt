package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Trade
import com.example.data.model.TradeResult
import com.example.domain.TradeCsvExporter
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.LiquidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs

@Composable
fun ExportCsvDialog(
    trades: List<Trade>,
    onDismiss: () -> Unit
) {
    val colors = LiquidTheme.colors
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var showPreview by remember { mutableStateOf(false) }
    var isCopied by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    val defaultFilename = remember { TradeCsvExporter.generateFilename() }
    val csvContent = remember(trades) { TradeCsvExporter.generateCsv(trades) }

    // SAF Document Creator to save directly into user's storage
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(csvContent.toByteArray(Charsets.UTF_8))
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "CSV saved successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to save CSV: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // Modal Dialog Entrance Animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "export_dialog_scale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(180),
        label = "export_dialog_alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
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
                .testTag("export_csv_dialog"),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp),
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
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = colors.indigoAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Export Trade History",
                                    color = colors.textPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Standard RFC-4180 CSV format",
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
                                .testTag("btn_close_export_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Scrollable Dialog Body
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Summary Stats Card
                        val wins = trades.count { it.result == TradeResult.WIN }
                        val losses = trades.count { it.result == TradeResult.LOSS }
                        val netPnl = trades.sumOf { it.effectivePnl }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (colors.isDark) Color(0x16FFFFFF) else Color(0x0E000000))
                                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "DATA SUMMARY",
                                        color = colors.textMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = defaultFilename,
                                        color = colors.indigoAccent,
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    ExportStatItem(label = "Total Trades", value = trades.size.toString(), color = colors.textPrimary)
                                    ExportStatItem(label = "Wins", value = wins.toString(), color = colors.emeraldWin)
                                    ExportStatItem(label = "Losses", value = losses.toString(), color = colors.crimsonLoss)
                                    ExportStatItem(
                                        label = "Net P&L",
                                        value = String.format(Locale.US, "%s$%,.2f", if (netPnl >= 0) "+" else "-", abs(netPnl)),
                                        color = if (netPnl >= 0) colors.emeraldWin else colors.crimsonLoss
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Included Columns List
                        Text(
                            text = "Included Fields (14 Columns):",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Trade ID, Date, Timestamp, Symbol, Direction, Result, Entry Price, Exit/Target Price, Stop Loss, Take Profit, Risk:Reward Ratio, Net PnL ($), Strategy, Notes",
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Preview Expandable Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (colors.isDark) Color(0x10FFFFFF) else Color(0x08000000))
                                .clickable { showPreview = !showPreview }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Preview,
                                    contentDescription = null,
                                    tint = colors.indigoAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (showPreview) "Hide CSV Preview" else "Show CSV Raw Preview",
                                    color = colors.indigoAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = if (showPreview) "▲" else "▼",
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                        }

                        AnimatedVisibility(visible = showPreview) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(1.dp, Color(0x336366F1), RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    val horizontalScroll = rememberScrollState()
                                    val verticalScroll = rememberScrollState()
                                    Text(
                                        text = csvContent,
                                        color = Color(0xFF67E8F9),
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 15.sp,
                                        modifier = Modifier
                                            .horizontalScroll(horizontalScroll)
                                            .verticalScroll(verticalScroll)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Action Buttons: Share, Save File, Copy
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. Primary Share Button (Sends CSV file via Android Share sheet to Excel, Sheets, Drive, etc.)
                        GlassButton(
                            text = "Share / Open in Excel or Sheets",
                            icon = Icons.Default.Share,
                            onClick = {
                                if (trades.isEmpty()) {
                                    Toast.makeText(context, "No trades to export yet.", Toast.LENGTH_SHORT).show()
                                    return@GlassButton
                                }
                                isExporting = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val file = TradeCsvExporter.writeCsvToCache(context, trades)
                                        val shareIntent = TradeCsvExporter.createShareIntent(context, file)
                                        withContext(Dispatchers.Main) {
                                            isExporting = false
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Trade History CSV"))
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isExporting = false
                                            Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            isLoading = isExporting,
                            accentGradient = listOf(colors.indigoDark, colors.indigoAccent),
                            testTag = "btn_share_csv"
                        )

                        // 2. Secondary Row: Save to Device + Copy to Clipboard
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Save to Device Button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (colors.isDark) Color(0x18FFFFFF) else Color(0x10000000))
                                    .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                                    .clickable {
                                        if (trades.isEmpty()) {
                                            Toast.makeText(context, "No trades to export yet.", Toast.LENGTH_SHORT).show()
                                            return@clickable
                                        }
                                        createDocumentLauncher.launch(defaultFilename)
                                    }
                                    .testTag("btn_save_csv_file")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Save File",
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Copy Raw CSV Button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isCopied) colors.emeraldWin.copy(alpha = 0.2f) else if (colors.isDark) Color(0x18FFFFFF) else Color(0x10000000))
                                    .border(1.dp, if (isCopied) colors.emeraldWin else colors.border, RoundedCornerShape(14.dp))
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(csvContent))
                                        isCopied = true
                                        Toast.makeText(context, "CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                    .testTag("btn_copy_csv_clipboard")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = if (isCopied) colors.emeraldWin else colors.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isCopied) "Copied!" else "Copy CSV",
                                        color = if (isCopied) colors.emeraldWin else colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportStatItem(
    label: String,
    value: String,
    color: Color
) {
    val colors = LiquidTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = colors.textMuted, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
