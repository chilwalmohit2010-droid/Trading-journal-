package com.example.domain

import com.example.data.model.Trade
import com.example.data.model.TradeDirection
import com.example.data.model.TradeResult
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class StrategyStats(
    val strategy: String,
    val totalTrades: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Double,
    val netPnl: Double,
    val profitFactor: Double
)

data class PairStats(
    val pair: String,
    val totalTrades: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Double,
    val netPnl: Double
)

data class SessionStats(
    val session: String,
    val totalTrades: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Double,
    val netPnl: Double
)

data class EmotionStats(
    val emotion: String,
    val totalTrades: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Double,
    val netPnl: Double
)

data class AdvancedAnalyticsReport(
    val totalTrades: Int,
    val settledTrades: Int,
    val winRate: Double,
    val totalNetPnl: Double,
    val grossProfit: Double,
    val grossLoss: Double,
    val profitFactor: Double,
    val averageWin: Double,
    val averageLoss: Double,
    val winLossRatio: Double,
    val maxDrawdownAmount: Double,
    val maxDrawdownPercent: Double,
    val expectancyPerTrade: Double,
    val longWinRate: Double,
    val shortWinRate: Double,
    val strategyBreakdown: List<StrategyStats>,
    val pairBreakdown: List<PairStats>,
    val sessionBreakdown: List<SessionStats>,
    val emotionBreakdown: List<EmotionStats>
)

object AnalyticsCalculator {

    fun determineSession(timestamp: Long): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = timestamp
        }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 13..16 -> "London/NY Overlap"
            hour in 8..16 -> "London"
            hour in 13..21 -> "New York"
            hour in 0..8 -> "Asian"
            else -> "Off-Hours"
        }
    }

    fun calculateReport(trades: List<Trade>): AdvancedAnalyticsReport {
        if (trades.isEmpty()) {
            return AdvancedAnalyticsReport(
                totalTrades = 0,
                settledTrades = 0,
                winRate = 0.0,
                totalNetPnl = 0.0,
                grossProfit = 0.0,
                grossLoss = 0.0,
                profitFactor = 0.0,
                averageWin = 0.0,
                averageLoss = 0.0,
                winLossRatio = 0.0,
                maxDrawdownAmount = 0.0,
                maxDrawdownPercent = 0.0,
                expectancyPerTrade = 0.0,
                longWinRate = 0.0,
                shortWinRate = 0.0,
                strategyBreakdown = emptyList(),
                pairBreakdown = emptyList(),
                sessionBreakdown = emptyList(),
                emotionBreakdown = emptyList()
            )
        }

        val total = trades.size
        val winsList = trades.filter { it.result == TradeResult.WIN }
        val lossesList = trades.filter { it.result == TradeResult.LOSS }
        val settled = winsList.size + lossesList.size
        val winRate = if (settled > 0) (winsList.size.toDouble() / settled) * 100.0 else 0.0

        val grossProfit = winsList.sumOf { abs(it.effectivePnl) }
        val grossLoss = lossesList.sumOf { abs(it.effectivePnl) }
        val totalNetPnl = trades.sumOf { it.effectivePnl }
        val profitFactor = if (grossLoss > 0.0001) grossProfit / grossLoss else if (grossProfit > 0) 99.9 else 0.0

        val avgWin = if (winsList.isNotEmpty()) grossProfit / winsList.size else 0.0
        val avgLoss = if (lossesList.isNotEmpty()) grossLoss / lossesList.size else 0.0
        val winLossRatio = if (avgLoss > 0.0001) avgWin / avgLoss else 0.0

        // Expectancy: (Win Rate * Avg Win) - (Loss Rate * Avg Loss)
        val winProb = if (settled > 0) winsList.size.toDouble() / settled else 0.0
        val lossProb = if (settled > 0) lossesList.size.toDouble() / settled else 0.0
        val expectancy = (winProb * avgWin) - (lossProb * avgLoss)

        // Direction win rates
        val longs = trades.filter { it.direction == TradeDirection.LONG && (it.result == TradeResult.WIN || it.result == TradeResult.LOSS) }
        val shorts = trades.filter { it.direction == TradeDirection.SHORT && (it.result == TradeResult.WIN || it.result == TradeResult.LOSS) }
        val longWinRate = if (longs.isNotEmpty()) (longs.count { it.result == TradeResult.WIN }.toDouble() / longs.size) * 100.0 else 0.0
        val shortWinRate = if (shorts.isNotEmpty()) (shorts.count { it.result == TradeResult.WIN }.toDouble() / shorts.size) * 100.0 else 0.0

        // Max Drawdown calculation from cumulative equity curve
        var peak = 0.0
        var maxDrawdown = 0.0
        var maxDrawdownPct = 0.0
        var runningEquity = 10000.0 // Baseline account
        var peakEquity = runningEquity

        trades.sortedBy { it.timestamp }.forEach { trade ->
            runningEquity += trade.effectivePnl
            if (runningEquity > peakEquity) {
                peakEquity = runningEquity
            }
            val dd = peakEquity - runningEquity
            if (dd > maxDrawdown) {
                maxDrawdown = dd
                val ddPct = if (peakEquity > 0) (dd / peakEquity) * 100.0 else 0.0
                if (ddPct > maxDrawdownPct) maxDrawdownPct = ddPct
            }
        }

        // 1. Strategy Breakdown
        val strategyMap = trades.groupBy { it.strategy.ifBlank { "Uncategorized" } }
        val strategyBreakdown = strategyMap.map { (strat, list) ->
            val w = list.count { it.result == TradeResult.WIN }
            val l = list.count { it.result == TradeResult.LOSS }
            val count = list.size
            val sSettled = w + l
            val sWinRate = if (sSettled > 0) (w.toDouble() / sSettled) * 100.0 else 0.0
            val sPnl = list.sumOf { it.effectivePnl }
            val sGp = list.filter { it.result == TradeResult.WIN }.sumOf { abs(it.effectivePnl) }
            val sGl = list.filter { it.result == TradeResult.LOSS }.sumOf { abs(it.effectivePnl) }
            val sPf = if (sGl > 0.0001) sGp / sGl else if (sGp > 0) 99.9 else 0.0
            StrategyStats(
                strategy = strat,
                totalTrades = count,
                wins = w,
                losses = l,
                winRate = sWinRate,
                netPnl = sPnl,
                profitFactor = sPf
            )
        }.sortedByDescending { it.netPnl }

        // 2. Pair Breakdown
        val pairMap = trades.groupBy { it.symbol.ifBlank { "Unknown" } }
        val pairBreakdown = pairMap.map { (symbol, list) ->
            val w = list.count { it.result == TradeResult.WIN }
            val l = list.count { it.result == TradeResult.LOSS }
            val count = list.size
            val pSettled = w + l
            val pWinRate = if (pSettled > 0) (w.toDouble() / pSettled) * 100.0 else 0.0
            val pPnl = list.sumOf { it.effectivePnl }
            PairStats(
                pair = symbol,
                totalTrades = count,
                wins = w,
                losses = l,
                winRate = pWinRate,
                netPnl = pPnl
            )
        }.sortedByDescending { it.netPnl }

        // 3. Session Breakdown
        val sessionMap = trades.groupBy {
            if (it.session.isNotBlank()) it.session else determineSession(it.timestamp)
        }
        val sessionBreakdown = sessionMap.map { (sess, list) ->
            val w = list.count { it.result == TradeResult.WIN }
            val l = list.count { it.result == TradeResult.LOSS }
            val count = list.size
            val seSettled = w + l
            val seWinRate = if (seSettled > 0) (w.toDouble() / seSettled) * 100.0 else 0.0
            val sePnl = list.sumOf { it.effectivePnl }
            SessionStats(
                session = sess,
                totalTrades = count,
                wins = w,
                losses = l,
                winRate = seWinRate,
                netPnl = sePnl
            )
        }.sortedByDescending { it.netPnl }

        // 4. Emotion Breakdown
        val emotionMap = trades.groupBy { it.emotion.ifBlank { "Neutral / Unspecified" } }
        val emotionBreakdown = emotionMap.map { (emo, list) ->
            val w = list.count { it.result == TradeResult.WIN }
            val l = list.count { it.result == TradeResult.LOSS }
            val count = list.size
            val eSettled = w + l
            val eWinRate = if (eSettled > 0) (w.toDouble() / eSettled) * 100.0 else 0.0
            val ePnl = list.sumOf { it.effectivePnl }
            EmotionStats(
                emotion = emo,
                totalTrades = count,
                wins = w,
                losses = l,
                winRate = eWinRate,
                netPnl = ePnl
            )
        }.sortedByDescending { it.netPnl }

        return AdvancedAnalyticsReport(
            totalTrades = total,
            settledTrades = settled,
            winRate = winRate,
            totalNetPnl = totalNetPnl,
            grossProfit = grossProfit,
            grossLoss = grossLoss,
            profitFactor = profitFactor,
            averageWin = avgWin,
            averageLoss = avgLoss,
            winLossRatio = winLossRatio,
            maxDrawdownAmount = maxDrawdown,
            maxDrawdownPercent = maxDrawdownPct,
            expectancyPerTrade = expectancy,
            longWinRate = longWinRate,
            shortWinRate = shortWinRate,
            strategyBreakdown = strategyBreakdown,
            pairBreakdown = pairBreakdown,
            sessionBreakdown = sessionBreakdown,
            emotionBreakdown = emotionBreakdown
        )
    }
}
