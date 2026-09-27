package com.example.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Trade
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility to export trade history records to standard RFC-4180 CSV format
 * for external quantitative analysis, spreadsheets (Excel, Google Sheets),
 * or Python/Pandas workflows.
 */
object TradeCsvExporter {

    private val csvDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /**
     * Converts a list of trades into a formatted CSV string.
     */
    fun generateCsv(trades: List<Trade>): String {
        val sb = StringBuilder()

        // CSV Header
        sb.append(
            listOf(
                "Trade ID",
                "Date",
                "Timestamp (ms)",
                "Symbol",
                "Direction",
                "Result",
                "Entry Price",
                "Exit / Target Price",
                "Stop Loss",
                "Take Profit",
                "Risk:Reward Ratio",
                "Net PnL ($)",
                "Strategy",
                "Notes"
            ).joinToString(",") { escapeCsv(it) }
        ).append("\r\n")

        // Data rows sorted chronologically
        trades.sortedBy { it.timestamp }.forEach { trade ->
            val dateStr = csvDateFormat.format(Date(trade.timestamp))
            val entryStr = if (trade.entryPrice > 0) String.format(Locale.US, "%.4f", trade.entryPrice) else "0.00"
            val exitStr = if (trade.exitPrice > 0) String.format(Locale.US, "%.4f", trade.exitPrice) else "0.00"
            val slStr = if (trade.stopLoss > 0) String.format(Locale.US, "%.4f", trade.stopLoss) else ""
            val tpStr = if (trade.takeProfit > 0) String.format(Locale.US, "%.4f", trade.takeProfit) else ""
            val rrStr = if (trade.riskRewardRatio > 0) String.format(Locale.US, "%.2f", trade.riskRewardRatio) else ""
            val pnlStr = String.format(Locale.US, "%.2f", trade.effectivePnl)

            val row = listOf(
                trade.id,
                dateStr,
                trade.timestamp.toString(),
                trade.symbol,
                trade.direction.name,
                trade.result.name,
                entryStr,
                exitStr,
                slStr,
                tpStr,
                rrStr,
                pnlStr,
                trade.strategy,
                trade.notes
            ).joinToString(",") { escapeCsv(it) }

            sb.append(row).append("\r\n")
        }

        return sb.toString()
    }

    /**
     * Generates a recommended filename for the export.
     */
    fun generateFilename(): String {
        val stamp = fileTimestampFormat.format(Date())
        return "trades_export_$stamp.csv"
    }

    /**
     * Writes the CSV content to the app's cache directory and returns the File.
     */
    fun writeCsvToCache(context: Context, trades: List<Trade>): File {
        val exportDir = File(context.cacheDir, "exports").apply {
            if (!exists()) mkdirs()
        }
        val file = File(exportDir, generateFilename())
        FileWriter(file).use { writer ->
            writer.write(generateCsv(trades))
        }
        return file
    }

    /**
     * Creates an Intent to share or send the exported CSV file using FileProvider.
     */
    fun createShareIntent(context: Context, file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Trade History Log Export (${file.name})")
            putExtra(Intent.EXTRA_TEXT, "Here is my exported trade journal history log in CSV format.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * RFC-4180 CSV cell escaping.
     */
    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
