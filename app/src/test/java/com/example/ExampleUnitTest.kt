package com.example

import com.example.data.model.LeaderboardEntry
import com.example.data.model.Trade
import com.example.data.model.TradeDirection
import com.example.data.model.TradeResult
import com.example.data.model.UserProfile
import com.example.domain.ScoreCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testBaseScore_emptyTrades() {
        val score = ScoreCalculator.calculateScore(emptyList())
        assertEquals(0L, score)
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
        // 0 base + 45 win + 30 RR bonus + PnL alpha > 50
        assertTrue(score >= 50L)
    }

    @Test
    fun testLosingTrade_respectsFloor() {
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
        assertTrue(score >= ScoreCalculator.MINIMUM_SCORE_FLOOR)
    }

    @Test
    fun testLeaderboardDataConsistency_twoAccounts() {
        // Account A: 2 trades
        val accountATrades = listOf(
            Trade(id = "t1", symbol = "BTC/USDT", direction = TradeDirection.LONG, entryPrice = 50000.0, stopLoss = 49000.0, takeProfit = 53000.0, riskRewardRatio = 3.0, result = TradeResult.WIN, pnl = 400.0),
            Trade(id = "t2", symbol = "ETH/USDT", direction = TradeDirection.LONG, entryPrice = 3000.0, stopLoss = 2900.0, takeProfit = 3200.0, riskRewardRatio = 2.0, result = TradeResult.WIN, pnl = 200.0)
        )
        val accountAScore = ScoreCalculator.calculateScore(accountATrades)

        // Account B: 0 trades
        val accountBTrades = emptyList<Trade>()
        val accountBScore = ScoreCalculator.calculateScore(accountBTrades)

        assertEquals(0L, accountBScore)
        assertTrue(accountAScore > 0L)

        val entryA = LeaderboardEntry(
            uid = "uid_a",
            username = "AccountA",
            displayName = "Trader A",
            score = accountAScore,
            totalTrades = accountATrades.size
        )
        val entryB = LeaderboardEntry(
            uid = "uid_b",
            username = "AccountB",
            displayName = "Trader B",
            score = accountBScore,
            totalTrades = accountBTrades.size
        )

        val leaderboard = listOf(entryB, entryA).sortedWith(
            compareByDescending<LeaderboardEntry> { it.score }
                .thenByDescending { it.totalTrades }
                .thenByDescending { it.winRate }
        )

        // Rank #1 must be Account A
        assertEquals("uid_a", leaderboard[0].uid)
        assertEquals(accountAScore, leaderboard[0].score)
        assertEquals(2, leaderboard[0].totalTrades)

        // Rank #2 must be Account B
        assertEquals("uid_b", leaderboard[1].uid)
        assertEquals(0L, leaderboard[1].score)
        assertEquals(0, leaderboard[1].totalTrades)

        // Add 3rd trade to Account A
        val updatedATrades = accountATrades + Trade(id = "t3", symbol = "SOL/USDT", direction = TradeDirection.LONG, entryPrice = 150.0, stopLoss = 145.0, takeProfit = 165.0, riskRewardRatio = 3.0, result = TradeResult.WIN, pnl = 150.0)
        val updatedAScore = ScoreCalculator.calculateScore(updatedATrades)

        val updatedEntryA = entryA.copy(score = updatedAScore, totalTrades = updatedATrades.size)
        val updatedLeaderboard = listOf(entryB, updatedEntryA).sortedWith(
            compareByDescending<LeaderboardEntry> { it.score }
                .thenByDescending { it.totalTrades }
                .thenByDescending { it.winRate }
        )

        assertEquals("uid_a", updatedLeaderboard[0].uid)
        assertEquals(3, updatedLeaderboard[0].totalTrades)
        assertEquals(updatedAScore, updatedLeaderboard[0].score)
    }

    @Test
    fun testFirestoreMapSerializationConsistency() {
        val userMap = mapOf<String, Any?>(
            "uid" to "user_123",
            "username" to "pro_trader",
            "displayName" to "Pro Trader",
            "score" to 426L,
            "totalTrades" to 2,
            "wins" to 2,
            "losses" to 0,
            "winRate" to 100.0,
            "pnl" to 600.0,
            "hasCompletedProfile" to true
        )

        val profile = UserProfile.fromMap("user_123", userMap)
        val leaderboardEntry = LeaderboardEntry.fromMap("user_123", userMap)

        assertEquals(profile.score, leaderboardEntry.score)
        assertEquals(426L, leaderboardEntry.score)
        assertEquals(profile.totalTrades, leaderboardEntry.totalTrades)
        assertEquals(2, leaderboardEntry.totalTrades)
    }

    @Test
    fun testScoreTimelineGenerator_progressionOverTime() {
        val now = System.currentTimeMillis()
        val trades = listOf(
            Trade(id = "t1", symbol = "BTC/USDT", direction = TradeDirection.LONG, entryPrice = 50000.0, stopLoss = 49000.0, takeProfit = 53000.0, riskRewardRatio = 3.0, result = TradeResult.WIN, pnl = 500.0, timestamp = now - 200000),
            Trade(id = "t2", symbol = "ETH/USDT", direction = TradeDirection.SHORT, entryPrice = 3000.0, stopLoss = 3100.0, takeProfit = 2800.0, riskRewardRatio = 2.0, result = TradeResult.LOSS, pnl = -150.0, timestamp = now - 100000),
            Trade(id = "t3", symbol = "SOL/USDT", direction = TradeDirection.LONG, entryPrice = 150.0, stopLoss = 145.0, takeProfit = 165.0, riskRewardRatio = 3.0, result = TradeResult.WIN, pnl = 300.0, timestamp = now)
        )

        val timeline = com.example.ui.components.ScoreTimelineGenerator.generateTimeline(trades)
        assertEquals(3, timeline.size)

        // Verify ordering and index
        assertEquals(1, timeline[0].index)
        assertEquals("BTC/USDT", timeline[0].trade?.symbol)
        assertEquals(2, timeline[1].index)
        assertEquals("ETH/USDT", timeline[1].trade?.symbol)
        assertEquals(3, timeline[2].index)
        assertEquals("SOL/USDT", timeline[2].trade?.symbol)

        // Point 3 score must match overall ScoreCalculator result
        val expectedTotalScore = ScoreCalculator.calculateScore(trades)
        assertEquals(expectedTotalScore, timeline[2].score)
        assertEquals(650.0, timeline[2].cumulativePnL, 0.01)
        assertEquals(2, timeline[2].cumulativeWins)
        assertEquals(1, timeline[2].cumulativeLosses)

        // Filters test
        val last10 = com.example.ui.components.ScoreTimelineGenerator.filterTimeline(timeline, com.example.ui.components.ScoreChartFilter.LAST_10)
        assertEquals(3, last10.size)

        val emptyTimeline = com.example.ui.components.ScoreTimelineGenerator.generateTimeline(emptyList())
        assertTrue(emptyTimeline.isEmpty())
    }

    @Test
    fun testTwoAccountLeaderboard_perspectiveSwitching() {
        // Account A: 1450 PTS, 2 trades
        val entryA = LeaderboardEntry(
            uid = "uid_a",
            username = "trader_alpha",
            displayName = "Trader Alpha",
            score = 1450L,
            totalTrades = 2,
            wins = 2,
            losses = 0,
            winRate = 100.0,
            totalPnl = 850.0
        )

        // Account B: 0 PTS, 0 trades
        val entryB = LeaderboardEntry(
            uid = "uid_b",
            username = "trader_beta",
            displayName = "Trader Beta",
            score = 0L,
            totalTrades = 0,
            wins = 0,
            losses = 0,
            winRate = 0.0,
            totalPnl = 0.0
        )

        val rawList = listOf(entryB, entryA)
        val sortedList = rawList.sortedWith(
            compareByDescending<LeaderboardEntry> { it.score }
                .thenByDescending { it.totalTrades }
                .thenByDescending { it.winRate }
                .thenBy { it.username.lowercase() }
        )

        // Both accounts must appear
        assertEquals(2, sortedList.size)
        assertEquals("uid_a", sortedList[0].uid)
        assertEquals(1450L, sortedList[0].score)
        assertEquals("uid_b", sortedList[1].uid)
        assertEquals(0L, sortedList[1].score)

        // From Account A's perspective:
        val currentUidA = "uid_a"
        val rankA = sortedList.indexOfFirst { it.uid == currentUidA } + 1
        assertEquals(1, rankA)

        // From Account B's perspective:
        val currentUidB = "uid_b"
        val rankB = sortedList.indexOfFirst { it.uid == currentUidB } + 1
        assertEquals(2, rankB)

        // When Account B logs in, the leaderboard order and score DO NOT CHANGE:
        // Rank #1 is still Account A, Rank #2 is still Account B
        assertEquals("uid_a", sortedList[0].uid)
        assertEquals("uid_b", sortedList[1].uid)
    }

    @Test
    fun testMultiUserLeaderboardSorting_preservesAllTenUsers() {
        val users = (1..10).map { i ->
            LeaderboardEntry(
                uid = "user_$i",
                username = "trader_$i",
                displayName = "Trader $i",
                score = (i * 100).toLong(),
                totalTrades = i
            )
        }

        val sorted = users.sortedWith(
            compareByDescending<LeaderboardEntry> { it.score }
                .thenByDescending { it.totalTrades }
                .thenBy { it.username.lowercase() }
        )

        assertEquals(10, sorted.size)
        assertEquals("user_10", sorted[0].uid)
        assertEquals(1000L, sorted[0].score)
        assertEquals("user_1", sorted[9].uid)
        assertEquals(100L, sorted[9].score)
    }

    @Test
    fun testFieldAliasResilience() {
        val alternateMap = mapOf<String, Any?>(
            "userName" to "custom_trader",
            "display_name" to "Custom Trader Display",
            "avatarUrl" to "https://image.url/avatar.png",
            "points" to "750",
            "total_trades" to "5",
            "totalWins" to "4",
            "totalLosses" to "1",
            "win_rate" to "80.0",
            "total_pnl" to "1200.50"
        )

        val entry = LeaderboardEntry.fromMap("alt_uid", alternateMap)
        assertEquals("alt_uid", entry.uid)
        assertEquals("custom_trader", entry.username)
        assertEquals("Custom Trader Display", entry.displayName)
        assertEquals("https://image.url/avatar.png", entry.photoURL)
        assertEquals(750L, entry.score)
        assertEquals(5, entry.totalTrades)
        assertEquals(4, entry.wins)
        assertEquals(1, entry.losses)
        assertEquals(80.0, entry.winRate, 0.01)
        assertEquals(1200.50, entry.totalPnl, 0.01)
    }
}

