package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {
    @Query("SELECT * FROM cached_trades WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTradesForUser(userId: String): Flow<List<TradeEntity>>

    @Query("SELECT * FROM cached_trades WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getTradesForUserOnce(userId: String): List<TradeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrades(trades: List<TradeEntity>)

    @Query("DELETE FROM cached_trades WHERE id = :tradeId")
    suspend fun deleteTradeById(tradeId: String)

    @Query("DELETE FROM cached_trades WHERE userId = :userId")
    suspend fun clearUserTrades(userId: String)
}
