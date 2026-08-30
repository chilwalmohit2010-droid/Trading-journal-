package com.example.data.model

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val photoURL: String = "",
    val email: String = "",
    val score: Long = 1000L,
    val totalTrades: Int = 0,
    val wins: Int = 0,
    val pnl: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "username" to username,
        "displayName" to displayName,
        "bio" to bio,
        "photoURL" to photoURL,
        "email" to email,
        "score" to score,
        "totalTrades" to totalTrades,
        "wins" to wins,
        "pnl" to pnl,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(uid: String, map: Map<String, Any?>): UserProfile {
            val username = map["username"] as? String ?: "Trader"
            val displayName = map["displayName"] as? String ?: username
            val bio = map["bio"] as? String ?: ""
            val photoURL = map["photoURL"] as? String ?: (map["photoUrl"] as? String ?: "")
            val email = map["email"] as? String ?: ""
            val score = (map["score"] as? Number)?.toLong() ?: 1000L
            val totalTrades = (map["totalTrades"] as? Number)?.toInt() ?: 0
            val wins = (map["wins"] as? Number)?.toInt() ?: 0
            val pnl = (map["pnl"] as? Number)?.toDouble() ?: 0.0
            val createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()

            return UserProfile(
                uid = uid,
                username = username,
                displayName = displayName,
                bio = bio,
                photoURL = photoURL,
                email = email,
                score = score,
                totalTrades = totalTrades,
                wins = wins,
                pnl = pnl,
                createdAt = createdAt
            )
        }
    }
}

data class LeaderboardEntry(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val photoURL: String = "",
    val score: Long = 1000L,
    val totalTrades: Int = 0,
    val winRate: Double = 0.0,
    val totalPnl: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "username" to username,
        "displayName" to displayName,
        "photoURL" to photoURL,
        "score" to score,
        "totalTrades" to totalTrades,
        "winRate" to winRate,
        "totalPnl" to totalPnl,
        "updatedAt" to updatedAt
    )

    companion object {
        fun fromMap(uid: String, map: Map<String, Any?>): LeaderboardEntry {
            val username = map["username"] as? String ?: "Trader"
            val displayName = map["displayName"] as? String ?: username
            val photoURL = map["photoURL"] as? String ?: (map["photoUrl"] as? String ?: "")
            return LeaderboardEntry(
                uid = uid,
                username = username,
                displayName = displayName,
                photoURL = photoURL,
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
