package com.example

import com.example.data.model.Trade
import com.example.data.model.TradeDirection
import com.example.data.model.TradeResult
import com.example.domain.TradeCsvExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TradeCsvExporterTest {

    @Test
    fun generateCsv_withEmptyTrades_returnsHeaderOnly() {
        val csv = TradeCsvExporter.generateCsv(emptyList())
        val lines = csv.trim().lines()
        assertEquals(1, lines.size)
        assertTrue(lines[0].contains("Trade ID"))
        assertTrue(lines[0].contains("Symbol"))
        assertTrue(lines[0].contains("Net PnL ($)"))
    }

    @Test
    fun generateCsv_formatsTradeRowCorrectly() {
        val trade = Trade(
            id = "trade-123",
            symbol = "BTC/USDT",
            direction = TradeDirection.LONG,
            entryPrice = 64200.0,
            stopLoss = 63500.0,
            takeProfit = 66300.0,
            riskRewardRatio = 3.0,
            result = TradeResult.WIN,
            pnl = 2100.0,
            timestamp = 1700000000000L,
            strategy = "Breakout, 5m",
            notes = "Clean retest \"strong buyers\""
        )

        val csv = TradeCsvExporter.generateCsv(listOf(trade))
        val lines = csv.trim().lines()
        assertEquals(2, lines.size)

        val header = lines[0]
        val dataRow = lines[1]

        assertTrue(header.startsWith("Trade ID,Date,"))
        assertTrue(dataRow.contains("trade-123"))
        assertTrue(dataRow.contains("BTC/USDT"))
        assertTrue(dataRow.contains("LONG"))
        assertTrue(dataRow.contains("WIN"))
        assertTrue(dataRow.contains("64200.0000"))
        assertTrue(dataRow.contains("66300.0000"))
        assertTrue(dataRow.contains("63500.0000"))
        assertTrue(dataRow.contains("3.00"))
        assertTrue(dataRow.contains("2100.00"))
        // Strategy with comma should be wrapped in quotes
        assertTrue(dataRow.contains("\"Breakout, 5m\""))
        // Notes with quotes should be escaped with double quotes
        assertTrue(dataRow.contains("\"Clean retest \"\"strong buyers\"\"\""))
    }

    @Test
    fun generateCsv_sortsTradesChronologically() {
        val trade1 = Trade(id = "1", timestamp = 2000L, symbol = "ETH/USDT")
        val trade2 = Trade(id = "2", timestamp = 1000L, symbol = "BTC/USDT")

        val csv = TradeCsvExporter.generateCsv(listOf(trade1, trade2))
        val lines = csv.trim().lines()
        assertEquals(3, lines.size)

        // Older trade (timestamp 1000L) should appear before newer trade (timestamp 2000L)
        assertTrue(lines[1].contains("BTC/USDT"))
        assertTrue(lines[2].contains("ETH/USDT"))
    }
}
