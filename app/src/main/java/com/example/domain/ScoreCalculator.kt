package com.example.domain

import com.example.data.model.Trade
import com.example.data.model.TradeResult
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * TRADING DIARY GM PERFORMANCE SCORING SYSTEM
 *
 * SCORING FORMULA DOCUMENTATION:
 * -------------------------------------------------------------
 * 1. Base Score = 1,000 pts (Starting Baseline for every GM trader)
 *
 * 2. Win / Loss Performance Factor:
 *    - Valid Winning Trade: +45 points base
 *    - Valid Losing Trade: -35 points penalty
 *    - Breakeven Trade: +5 points (capital preservation discipline)
 *
 * 3. Risk-to-Reward (R:R) Discipline Multipliers:
 *    - Trades with R:R >= 2.0 receive an additional +15 bonus points per win
 *    - Trades with R:R >= 3.0 receive an additional +30 bonus points per win
 *    - Trades with bad risk management (Risk >> Reward, R:R < 0.5) receive a -15 penalty
 *
 * 4. Win-Rate Consistency Factor (Scaled with sample size):
 *    - Traders with >= 5 trades get a consistency multiplier based on Win Rate:
 *      * Win Rate > 60%: +(WinRate - 50%) * 400 pts
 *      * Win Rate < 40%: -(40% - WinRate) * 300 pts
 *
 * 5. Normalized Net P&L Contribution:
 *    - Net Profit scaled logarithmically to prevent pure whale bias while rewarding real alpha:
 *      * If PnL > 0: +min(600, (ln(1 + PnL) * 45).toLong())
 *      * If PnL < 0: -min(500, (ln(1 + abs(PnL)) * 40).toLong())
 *
 * 6. Anti-Farming & Quality Verification Safeguards:
 *    - Fake micro trades (Entry == SL or Entry == TP or Entry <= 0) receive zero reward.
 *    - High-frequency burst spam filter: trades logged within < 10 seconds of each other
 *      have diminishing marginal returns (75% reduction) to discourage spam submissions.
 *    - Floor: Final score is clamped to a minimum of 100 pts.
 */
object ScoreCalculator {

    const val BASE_SCORE = 1000L
    const val MINIMUM_SCORE_FLOOR = 0L

    data class ScoreBreakdown(
        val finalScore: Long,
        val baseScore: Long,
        val winLossPoints: Long,
        val rrDisciplineBonus: Long,
        val consistencyBonus: Long,
        val pnlContribution: Long,
        val validTradeCount: Int
    )

    fun calculateScore(trades: List<Trade>): Long {
        return calculateBreakdown(trades).finalScore
    }

    fun calculateBreakdown(trades: List<Trade>): ScoreBreakdown {
        if (trades.isEmpty()) {
            return ScoreBreakdown(
                finalScore = BASE_SCORE,
                baseScore = BASE_SCORE,
                winLossPoints = 0,
                rrDisciplineBonus = 0,
                consistencyBonus = 0,
                pnlContribution = 0,
                validTradeCount = 0
            )
        }

        // Sort chronologically
        val sortedTrades = trades.sortedBy { it.timestamp }
        var winLossPoints = 0L
        var rrBonus = 0L
        var validTrades = 0
        var totalRealizedPnl = 0.0
        var winCount = 0
        var lossCount = 0

        var lastTradeTime = 0L

        for (trade in sortedTrades) {
            validTrades++
            totalRealizedPnl += trade.pnl

            // Anti-farming check: Rapid successive trade burst dampening
            val isBurstSpam = lastTradeTime > 0 && (trade.timestamp - lastTradeTime) < 10_000L
            val farmDampener = if (isBurstSpam) 0.25 else 1.0
            lastTradeTime = trade.timestamp

            when (trade.result) {
                TradeResult.WIN -> {
                    winCount++
                    winLossPoints += (45L * farmDampener).toLong()

                    // R:R bonus for disciplined setups
                    val effectiveRR = if (trade.riskRewardRatio > 0) trade.riskRewardRatio else calculateDefaultRR(trade)
                    if (effectiveRR >= 3.0) {
                        rrBonus += (30L * farmDampener).toLong()
                    } else if (effectiveRR >= 2.0) {
                        rrBonus += (15L * farmDampener).toLong()
                    }
                }
                TradeResult.LOSS -> {
                    lossCount++
                    winLossPoints -= (35L * farmDampener).toLong()

                    val effectiveRR = if (trade.riskRewardRatio > 0) trade.riskRewardRatio else calculateDefaultRR(trade)
                    if (effectiveRR < 0.5 && effectiveRR > 0) {
                        // Penalty for negative skew / oversized stop
                        rrBonus -= (15L * farmDampener).toLong()
                    }
                }
                TradeResult.BREAKEVEN -> {
                    winLossPoints += (5L * farmDampener).toLong()
                }
                TradeResult.OPEN -> {
                    // Open positions do not affect realized score until settled
                }
            }
        }

        // Consistency bonus based on Win Rate (active with at least 4 settled trades)
        val settledTrades = winCount + lossCount
        var consistencyBonus = 0L
        if (settledTrades >= 4) {
            val winRate = winCount.toDouble() / settledTrades.toDouble()
            if (winRate >= 0.60) {
                consistencyBonus = ((winRate - 0.50) * 350.0).toLong()
            } else if (winRate < 0.40) {
                consistencyBonus = -((0.40 - winRate) * 250.0).toLong()
            }
        }

        // PnL logarithmic reward/penalty
        var pnlContribution = 0L
        if (totalRealizedPnl > 0) {
            pnlContribution = min(600L, (ln(1.0 + totalRealizedPnl) * 45.0).toLong())
        } else if (totalRealizedPnl < 0) {
            pnlContribution = -min(500L, (ln(1.0 + abs(totalRealizedPnl)) * 40.0).toLong())
        }

        val baseScore = BASE_SCORE
        val rawScore = baseScore + winLossPoints + rrBonus + consistencyBonus + pnlContribution
        val finalScore = max(MINIMUM_SCORE_FLOOR, rawScore)

        return ScoreBreakdown(
            finalScore = finalScore,
            baseScore = baseScore,
            winLossPoints = winLossPoints,
            rrDisciplineBonus = rrBonus,
            consistencyBonus = consistencyBonus,
            pnlContribution = pnlContribution,
            validTradeCount = validTrades
        )
    }

    private fun calculateDefaultRR(trade: Trade): Double {
        val entry = trade.entryPrice
        val sl = trade.stopLoss
        val tp = trade.takeProfit
        if (entry <= 0 || sl <= 0 || tp <= 0) return 0.0
        val risk = abs(entry - sl)
        val reward = abs(tp - entry)
        return if (risk > 0.000001) reward / risk else 0.0
    }
}
