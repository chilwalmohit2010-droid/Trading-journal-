package com.example.data.model

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "username" to username,
        "email" to email,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(uid: String, map: Map<String, Any?>): UserProfile {
            return UserProfile(
                uid = uid,
                username = map["username"] as? String ?: "Trader",
                email = map["email"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

data class LeaderboardEntry(
    val uid: String = "",
    val username: String = "",
    val score: Long = 1000L,
    val totalTrades: Int = 0,
    val winRate: Double = 0.0,
    val totalPnl: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "username" to username,
        "score" to score,
        "totalTrades" to totalTrades,
        "winRate" to winRate,
        "totalPnl" to totalPnl,
        "updatedAt" to updatedAt
    )

    companion object {
        fun fromMap(uid: String, map: Map<String, Any?>): LeaderboardEntry {
            return LeaderboardEntry(
                uid = uid,
                username = map["username"] as? String ?: "Trader",
                score = (map["score"] as? Number)?.toLong() ?: 1000L,
                totalTrades = (map["totalTrades"] as? Number)?.toInt() ?: 0,
                winRate = (map["winRate"] as? Number)?.toDouble() ?: 0.0,
                totalPnl = (map["totalPnl"] as? Number)?.toDouble() ?: 0.0,
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

data class TradingStats(
    val totalTrades: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val breakevens: Int = 0,
    val winRate: Double = 0.0,
    val totalPnl: Double = 0.0,
    val avgRiskReward: Double = 0.0,
    val profitFactor: Double = 0.0,
    val currentScore: Long = 1000L,
    val rank: Int = 1,
    val totalTradersCount: Int = 1
)
