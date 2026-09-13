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
        // 1000 base + 45 win + 30 RR bonus + PnL alpha > 1050
        assertTrue(score >= 1050L)
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
    fun testLeaderboardDataConsistency_threeAccountsDebugScenario() {
        // Account A: 3 trades, real score 1394
        val entryA = LeaderboardEntry(
            uid = "uid_a",
            username = "AccountA",
            displayName = "Account A",
            score = 1394L,
            totalTrades = 3,
            wins = 3,
            losses = 0,
            winRate = 100.0,
            totalPnl = 800.0
        )

        // Account B: 0 trades, score 1000
        val entryB = LeaderboardEntry(
            uid = "uid_b",
            username = "AccountB",
            displayName = "Account B",
            score = 1000L,
            totalTrades = 0,
            wins = 0,
            losses = 0,
            winRate = 0.0,
            totalPnl = 0.0
        )

        val initialList = listOf(entryB, entryA).sortedWith(
            compareByDescending<LeaderboardEntry> { it.score }
                .thenByDescending { it.totalTrades }
                .thenByDescending { it.winRate }
                .thenByDescending { it.updatedAt }
                .thenBy { it.username.lowercase() }
        )

        // 1. Account A must be #1 (1394 PTS, 3 trades), Account B must be #2 (1000 PTS, 0 trades)
        assertEquals(2, initialList.size)
        assertEquals("uid_a", initialList[0].uid)
        assertEquals(1394L, initialList[0].score)
        assertEquals(3, initialList[0].totalTrades)
        assertEquals(1, initialList.indexOfFirst { it.uid == "uid_a" } + 1)

        assertEquals("uid_b", initialList[1].uid)
        assertEquals(1000L, initialList[1].score)
        assertEquals(0, initialList[1].totalTrades)
        assertEquals(2, initialList.indexOfFirst { it.uid == "uid_b" } + 1)

        // 2. If Account B has a higher score (e.g. 1500), Account B becomes #1
        val updatedEntryB = entryB.copy(score = 1500L, totalTrades = 2)
        val swappedList = listOf(entryA, updatedEntryB).sortedWith(
            compareByDescending<LeaderboardEntry> { it.score }
                .thenByDescending { it.totalTrades }
                .thenByDescending { it.winRate }
                .thenByDescending { it.updatedAt }
                .thenBy { it.username.lowercase() }
        )
        assertEquals("uid_b", swappedList[0].uid)
        assertEquals(1500L, swappedList[0].score)
        assertEquals(1, swappedList.indexOfFirst { it.uid == "uid_b" } + 1)

        // 3. Create a third account with 0 trades (score 1000)
        val entryC = LeaderboardEntry(
            uid = "uid_c",
            username = "AccountC",
            displayName = "Account C",
            score = 1000L,
            totalTrades = 0,
            wins = 0,
            losses = 0,
            winRate = 0.0,
            totalPnl = 0.0,
            updatedAt = System.currentTimeMillis() - 10000 // slightly older or stable
        )
        val threeAccountList = listOf(entryA, entryB, entryC).sortedWith(
            compareByDescending<LeaderboardEntry> { it.score }
                .thenByDescending { it.totalTrades }
                .thenByDescending { it.winRate }
                .thenByDescending { it.updatedAt }
                .thenBy { it.username.lowercase() }
        )
        assertEquals(3, threeAccountList.size)
        assertEquals("uid_a", threeAccountList[0].uid)
        assertEquals(1, threeAccountList.indexOfFirst { it.uid == "uid_a" } + 1)
        assertEquals(2, threeAccountList.indexOfFirst { it.uid == "uid_b" } + 1)
        assertEquals(3, threeAccountList.indexOfFirst { it.uid == "uid_c" } + 1)
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

        // Account B: 1000 PTS, 0 trades
        val entryB = LeaderboardEntry(
            uid = "uid_b",
            username = "trader_beta",
            displayName = "Trader Beta",
            score = 1000L,
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
        assertEquals(1000L, sortedList[1].score)

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

    @Test
    fun testLossMakingTrade_isSubtractedFromNetPnl() {
        // Test winning trade (+500) and loss-making trade (-200, entered as 200)
        val winTrade = Trade(
            id = "w1",
            symbol = "BTC/USDT",
            result = TradeResult.WIN,
            pnl = 500.0
        )
        val rawLossTrade = Trade(
            id = "l1",
            symbol = "ETH/USDT",
            result = TradeResult.LOSS,
            pnl = 200.0
        )

        // effectivePnl and normalized() guarantee negative sign for LOSS
        assertEquals(500.0, winTrade.effectivePnl, 0.001)
        assertEquals(-200.0, rawLossTrade.effectivePnl, 0.001)

        val normalizedLoss = rawLossTrade.normalized()
        assertEquals(-200.0, normalizedLoss.pnl, 0.001)

        // fromMap also ensures normalized sign
        val reconstructedLoss = Trade.fromMap("l1", mapOf("result" to "LOSS", "pnl" to 200.0))
        assertEquals(-200.0, reconstructedLoss.pnl, 0.001)

        // toMap exports negative PnL to Firestore
        assertEquals(-200.0, (rawLossTrade.toMap()["pnl"] as Number).toDouble(), 0.001)

        // Sum of net PnL: 500 - 200 = 300 (Loss is subtracted, NEVER added!)
        val trades = listOf(winTrade, normalizedLoss)
        val netPnl = trades.sumOf { it.pnl }
        assertEquals(300.0, netPnl, 0.001)
    }

    @Test
    fun testMultipleLossTrades_properlyAccumulateNegativePnl() {
        val loss1 = Trade.fromMap("l1", mapOf("symbol" to "SOL/USDT", "result" to "LOSS", "pnl" to 150.0))
        val loss2 = Trade(id = "l2", symbol = "EUR/USD", result = TradeResult.LOSS, pnl = -350.0).normalized()

        assertEquals(-150.0, loss1.pnl, 0.001)
        assertEquals(-350.0, loss2.pnl, 0.001)

        val totalPnl = listOf(loss1, loss2).sumOf { it.pnl }
        assertEquals(-500.0, totalPnl, 0.001)
    }
}

