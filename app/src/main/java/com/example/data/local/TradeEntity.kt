package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Trade
import com.example.data.model.TradeDirection
import com.example.data.model.TradeResult

@Entity(tableName = "cached_trades")
data class TradeEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val symbol: String,
    val direction: String,
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val riskRewardRatio: Double,
    val result: String,
    val pnl: Double,
    val timestamp: Long,
    val notes: String,
    val strategy: String,
    val screenshotUri: String = "",
    val exitScreenshotUri: String = "",
    val emotion: String = "",
    val session: String = "",
    val checklistScore: Int = 0
) {
    fun toTrade(): Trade {
        val parsedResult = try { TradeResult.valueOf(result) } catch (e: Exception) { TradeResult.WIN }
        val normalizedPnl = when (parsedResult) {
            TradeResult.LOSS -> -kotlin.math.abs(pnl)
            TradeResult.WIN -> kotlin.math.abs(pnl)
            TradeResult.BREAKEVEN -> 0.0
            TradeResult.OPEN -> pnl
        }
        return Trade(
            id = id,
            userId = userId,
            symbol = symbol,
            direction = try { TradeDirection.valueOf(direction) } catch (e: Exception) { TradeDirection.LONG },
            entryPrice = entryPrice,
            stopLoss = stopLoss,
            takeProfit = takeProfit,
            riskRewardRatio = riskRewardRatio,
            result = parsedResult,
            pnl = normalizedPnl,
            timestamp = timestamp,
            notes = notes,
            strategy = strategy,
            screenshotUri = screenshotUri,
            exitScreenshotUri = exitScreenshotUri,
            emotion = emotion,
            session = session,
            checklistScore = checklistScore
        )
    }

    companion object {
        fun fromTrade(trade: Trade): TradeEntity {
            val normalizedPnl = when (trade.result) {
                TradeResult.LOSS -> -kotlin.math.abs(trade.pnl)
                TradeResult.WIN -> kotlin.math.abs(trade.pnl)
                TradeResult.BREAKEVEN -> 0.0
                TradeResult.OPEN -> trade.pnl
            }
            return TradeEntity(
                id = trade.id,
                userId = trade.userId,
                symbol = trade.symbol,
                direction = trade.direction.name,
                entryPrice = trade.entryPrice,
                stopLoss = trade.stopLoss,
                takeProfit = trade.takeProfit,
                riskRewardRatio = trade.riskRewardRatio,
                result = trade.result.name,
                pnl = normalizedPnl,
                timestamp = trade.timestamp,
                notes = trade.notes,
                strategy = trade.strategy,
                screenshotUri = trade.screenshotUri,
                exitScreenshotUri = trade.exitScreenshotUri,
                emotion = trade.emotion,
                session = trade.session,
                checklistScore = trade.checklistScore
            )
        }
    }
}
