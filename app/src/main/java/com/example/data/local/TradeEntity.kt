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
    val strategy: String
) {
    fun toTrade(): Trade = Trade(
        id = id,
        userId = userId,
        symbol = symbol,
        direction = try { TradeDirection.valueOf(direction) } catch (e: Exception) { TradeDirection.LONG },
        entryPrice = entryPrice,
        stopLoss = stopLoss,
        takeProfit = takeProfit,
        riskRewardRatio = riskRewardRatio,
        result = try { TradeResult.valueOf(result) } catch (e: Exception) { TradeResult.WIN },
        pnl = pnl,
        timestamp = timestamp,
        notes = notes,
        strategy = strategy
    )

    companion object {
        fun fromTrade(trade: Trade): TradeEntity = TradeEntity(
            id = trade.id,
            userId = trade.userId,
            symbol = trade.symbol,
            direction = trade.direction.name,
            entryPrice = trade.entryPrice,
            stopLoss = trade.stopLoss,
            takeProfit = trade.takeProfit,
            riskRewardRatio = trade.riskRewardRatio,
            result = trade.result.name,
            pnl = trade.pnl,
            timestamp = trade.timestamp,
            notes = trade.notes,
            strategy = trade.strategy
        )
    }
}
