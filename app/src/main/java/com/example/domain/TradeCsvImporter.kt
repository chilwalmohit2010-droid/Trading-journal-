package com.example.domain

import com.example.data.model.Trade
import com.example.data.model.TradeDirection
import com.example.data.model.TradeResult
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

object TradeCsvImporter {

    private val supportedDateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US),
        SimpleDateFormat("MM/dd/yyyy", Locale.US),
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
    )

    /**
     * Parses CSV text into a list of Trade objects.
     */
    fun parseCsv(csvText: String, defaultUserId: String = ""): List<Trade> {
        val rawLines = splitCsvLines(csvText)
        if (rawLines.isEmpty()) return emptyList()

        val headerRow = parseCsvLine(rawLines[0]).map { it.trim().lowercase() }
        val trades = mutableListOf<Trade>()

        // Column index resolver
        fun findCol(vararg names: String): Int {
            for (name in names) {
                val idx = headerRow.indexOfFirst { it.contains(name.lowercase()) }
                if (idx >= 0) return idx
            }
            return -1
        }

        val idIdx = findCol("trade id", "id")
        val dateIdx = findCol("date", "time", "date/time")
        val timestampIdx = findCol("timestamp")
        val symbolIdx = findCol("symbol", "pair", "instrument", "asset", "ticker")
        val directionIdx = findCol("direction", "side", "type")
        val resultIdx = findCol("result", "outcome", "status")
        val entryIdx = findCol("entry price", "entry", "open price", "open")
        val exitIdx = findCol("exit / target price", "exit price", "exit", "close price", "target price", "target")
        val slIdx = findCol("stop loss", "sl")
        val tpIdx = findCol("take profit", "tp")
        val rrIdx = findCol("risk:reward", "risk reward", "rr", "r:r")
        val pnlIdx = findCol("net pnl", "pnl", "profit", "p/l", "net profit")
        val strategyIdx = findCol("strategy", "setup")
        val notesIdx = findCol("notes", "comment", "description")

        for (i in 1 until rawLines.size) {
            val line = rawLines[i]
            if (line.isBlank()) continue

            val cells = parseCsvLine(line)
            if (cells.isEmpty()) continue

            fun cell(idx: Int): String = if (idx in cells.indices) cells[idx].trim() else ""

            val symbol = if (symbolIdx >= 0) cell(symbolIdx).uppercase() else "BTC/USDT"
            if (symbol.isBlank()) continue

            // Parse ID
            val tradeId = if (idIdx >= 0 && cell(idIdx).isNotBlank()) cell(idIdx) else UUID.randomUUID().toString()

            // Parse Timestamp
            var timestamp = System.currentTimeMillis()
            if (timestampIdx >= 0 && cell(timestampIdx).toLongOrNull() != null) {
                timestamp = cell(timestampIdx).toLong()
            } else if (dateIdx >= 0 && cell(dateIdx).isNotBlank()) {
                val dateStr = cell(dateIdx)
                for (format in supportedDateFormats) {
                    try {
                        val parsed = format.parse(dateStr)
                        if (parsed != null) {
                            timestamp = parsed.time
                            break
                        }
                    } catch (_: Exception) {}
                }
            }

            // Direction
            val dirStr = if (directionIdx >= 0) cell(directionIdx).uppercase() else "LONG"
            val direction = if (dirStr.contains("SHORT") || dirStr.contains("SELL")) TradeDirection.SHORT else TradeDirection.LONG

            // Prices
            val entryPrice = if (entryIdx >= 0) cell(entryIdx).toDoubleOrNull() ?: 0.0 else 0.0
            val exitPrice = if (exitIdx >= 0) cell(exitIdx).toDoubleOrNull() ?: 0.0 else 0.0
            val stopLoss = if (slIdx >= 0) cell(slIdx).toDoubleOrNull() ?: 0.0 else 0.0
            val takeProfit = if (tpIdx >= 0) cell(tpIdx).toDoubleOrNull() ?: exitPrice else exitPrice

            // PnL
            val pnlRaw = if (pnlIdx >= 0) {
                cell(pnlIdx).replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
            } else {
                if (entryPrice > 0 && exitPrice > 0) {
                    if (direction == TradeDirection.LONG) exitPrice - entryPrice else entryPrice - exitPrice
                } else 0.0
            }

            // Result
            val resStr = if (resultIdx >= 0) cell(resultIdx).uppercase() else ""
            val result = when {
                resStr.contains("WIN") || resStr.contains("PROFIT") -> TradeResult.WIN
                resStr.contains("LOSS") -> TradeResult.LOSS
                resStr.contains("BREAKEVEN") || resStr.contains("BE") -> TradeResult.BREAKEVEN
                pnlRaw > 0.0001 -> TradeResult.WIN
                pnlRaw < -0.0001 -> TradeResult.LOSS
                else -> TradeResult.BREAKEVEN
            }

            // RR
            val rr = if (rrIdx >= 0) {
                cell(rrIdx).replace("1:", "").toDoubleOrNull() ?: 0.0
            } else {
                if (entryPrice > 0 && stopLoss > 0 && takeProfit > 0) {
                    val risk = abs(entryPrice - stopLoss)
                    val reward = abs(takeProfit - entryPrice)
                    if (risk > 0.0001) reward / risk else 0.0
                } else 0.0
            }

            val strategy = if (strategyIdx >= 0) cell(strategyIdx) else ""
            val notes = if (notesIdx >= 0) cell(notesIdx) else ""

            val normalizedPnl = when (result) {
                TradeResult.LOSS -> -abs(pnlRaw)
                TradeResult.WIN -> abs(pnlRaw)
                TradeResult.BREAKEVEN -> 0.0
                TradeResult.OPEN -> pnlRaw
            }

            trades.add(
                Trade(
                    id = tradeId,
                    userId = defaultUserId,
                    symbol = symbol,
                    direction = direction,
                    entryPrice = entryPrice,
                    stopLoss = stopLoss,
                    takeProfit = takeProfit,
                    riskRewardRatio = rr,
                    result = result,
                    pnl = normalizedPnl,
                    timestamp = timestamp,
                    notes = notes,
                    strategy = strategy
                )
            )
        }

        return trades
    }

    /**
     * Splits CSV into lines respecting quoted newlines.
     */
    private fun splitCsvLines(text: String): List<String> {
        val lines = mutableListOf<String>()
        val currentLine = StringBuilder()
        var inQuotes = false

        for (ch in text) {
            when (ch) {
                '"' -> {
                    inQuotes = !inQuotes
                    currentLine.append(ch)
                }
                '\n' -> {
                    if (inQuotes) {
                        currentLine.append(ch)
                    } else {
                        lines.add(currentLine.toString())
                        currentLine.clear()
                    }
                }
                '\r' -> {
                    // Ignore carriage returns
                }
                else -> currentLine.append(ch)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    /**
     * Parses a single CSV line into tokens, respecting quotes and double-quote escapes.
     */
    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val currentToken = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        currentToken.append('"')
                        i++ // Skip escaped quote
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    tokens.add(currentToken.toString())
                    currentToken.clear()
                }
                else -> currentToken.append(c)
            }
            i++
        }
        tokens.add(currentToken.toString())
        return tokens
    }
}
