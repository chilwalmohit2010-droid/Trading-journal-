package com.example.data.model

enum class TradeDirection {
    LONG,
    SHORT
}

enum class TradeResult {
    WIN,
    LOSS,
    BREAKEVEN,
    OPEN
}

data class Trade(
    val id: String = "",
    val userId: String = "",
    val symbol: String = "",
    val direction: TradeDirection = TradeDirection.LONG,
    val entryPrice: Double = 0.0,
    val stopLoss: Double = 0.0,
    val takeProfit: Double = 0.0,
    val riskRewardRatio: Double = 0.0,
    val result: TradeResult = TradeResult.WIN,
    val pnl: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val strategy: String = ""
) {
    /**
     * Exit price represented by takeProfit or close target.
     */
    val exitPrice: Double
        get() = takeProfit
    /**
     * Signed P&L guaranteed to be negative for LOSS, positive for WIN, 0.0 for BREAKEVEN.
     */
    val effectivePnl: Double
        get() = when (result) {
            TradeResult.LOSS -> -kotlin.math.abs(pnl)
            TradeResult.WIN -> kotlin.math.abs(pnl)
            TradeResult.BREAKEVEN -> 0.0
            TradeResult.OPEN -> pnl
        }

    fun normalized(): Trade = copy(pnl = effectivePnl)

    // Firestore conversion map with strictly normalized P&L sign
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "symbol" to symbol,
            "direction" to direction.name,
            "entryPrice" to entryPrice,
            "stopLoss" to stopLoss,
            "takeProfit" to takeProfit,
            "riskRewardRatio" to riskRewardRatio,
            "result" to result.name,
            "pnl" to effectivePnl,
            "timestamp" to timestamp,
            "notes" to notes,
            "strategy" to strategy
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Trade {
            val directionStr = map["direction"] as? String ?: TradeDirection.LONG.name
            val resultStr = map["result"] as? String ?: TradeResult.WIN.name
            val parsedResult = try { TradeResult.valueOf(resultStr) } catch (e: Exception) { TradeResult.WIN }
            val rawPnl = (map["pnl"] as? Number)?.toDouble() ?: 0.0
            val normalizedPnl = when (parsedResult) {
                TradeResult.LOSS -> -kotlin.math.abs(rawPnl)
                TradeResult.WIN -> kotlin.math.abs(rawPnl)
                TradeResult.BREAKEVEN -> 0.0
                TradeResult.OPEN -> rawPnl
            }
            return Trade(
                id = id,
                userId = map["userId"] as? String ?: "",
                symbol = map["symbol"] as? String ?: "",
                direction = try { TradeDirection.valueOf(directionStr) } catch (e: Exception) { TradeDirection.LONG },
                entryPrice = (map["entryPrice"] as? Number)?.toDouble() ?: 0.0,
                stopLoss = (map["stopLoss"] as? Number)?.toDouble() ?: 0.0,
                takeProfit = (map["takeProfit"] as? Number)?.toDouble() ?: 0.0,
                riskRewardRatio = (map["riskRewardRatio"] as? Number)?.toDouble() ?: 0.0,
                result = parsedResult,
                pnl = normalizedPnl,
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                notes = map["notes"] as? String ?: "",
                strategy = map["strategy"] as? String ?: ""
            )
        }
    }
}
