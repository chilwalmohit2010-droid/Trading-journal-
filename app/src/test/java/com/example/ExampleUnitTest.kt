package com.example

import com.example.data.model.Trade
import com.example.data.model.TradeDirection
import com.example.data.model.TradeResult
import com.example.domain.ScoreCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testBaseScore_emptyTrades() {
        val score = ScoreCalculator.calculateScore(emptyList())
        assertEquals(1000L, score)
    }

    @Test
    fun testWinningTrade_increasesScore() {
        val trades = listOf(
            Trade(
                id = "1",
                symbol = "BTC/USDT",
                direction = TradeDirection.LONG,
                entryPrice = 60000.0,
                stopLoss = 59000.0,
                takeProfit = 63000.0,
                riskRewardRatio = 3.0,
                result = TradeResult.WIN,
                pnl = 500.0
            )
        )
        val score = ScoreCalculator.calculateScore(trades)
        // 1000 base + 45 win + 30 RR bonus + PnL alpha > 1000
        assertTrue(score > 1050L)
    }

    @Test
    fun testLosingTrade_decreasesScore() {
        val trades = listOf(
            Trade(
                id = "2",
                symbol = "ETH/USDT",
                direction = TradeDirection.SHORT,
                entryPrice = 3000.0,
                stopLoss = 3100.0,
                takeProfit = 2800.0,
                riskRewardRatio = 2.0,
                result = TradeResult.LOSS,
                pnl = -200.0
            )
        )
        val score = ScoreCalculator.calculateScore(trades)
        assertTrue(score < 1000L)
        assertTrue(score >= ScoreCalculator.MINIMUM_SCORE_FLOOR)
    }
}
