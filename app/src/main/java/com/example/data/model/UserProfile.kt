package com.example.data.model

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val photoURL: String = "",
    val email: String = "",
    val score: Long = 0L,
    val totalTrades: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val pnl: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val hasCompletedProfile: Boolean = false
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
        "losses" to losses,
        "pnl" to pnl,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "hasCompletedProfile" to hasCompletedProfile
    )

    companion object {
        private fun parseLong(value: Any?, default: Long = 0L): Long {
            return when (value) {
                is Number -> value.toLong()
                is String -> value.toDoubleOrNull()?.toLong() ?: default
                else -> default
            }
        }

        private fun parseInt(value: Any?, default: Int = 0): Int {
            return when (value) {
                is Number -> value.toInt()
                is String -> value.toDoubleOrNull()?.toInt() ?: default
                else -> default
            }
        }

        private fun parseDouble(value: Any?, default: Double = 0.0): Double {
            return when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull() ?: default
                else -> default
            }
        }

        fun fromMap(uid: String, map: Map<String, Any?>): UserProfile {
            val username = (map["username"] as? String)
                ?: (map["userName"] as? String)
                ?: (map["displayName"] as? String)
                ?: (map["name"] as? String)
                ?: ""
            val displayName = (map["displayName"] as? String)
                ?: (map["display_name"] as? String)
                ?: (map["name"] as? String)
                ?: (if (username.isNotEmpty()) username else "Trader")
            val bio = (map["bio"] as? String) ?: ""
            val photoURL = (map["photoURL"] as? String)
                ?: (map["photoUrl"] as? String)
                ?: (map["photo_url"] as? String)
                ?: (map["avatar"] as? String)
                ?: (map["avatarUrl"] as? String)
                ?: ""
            val email = (map["email"] as? String) ?: ""
            val rawScore = parseLong(map["score"] ?: map["currentScore"] ?: map["points"] ?: map["totalScore"], 0L)
            val totalTrades = parseInt(map["totalTrades"] ?: map["total_trades"] ?: map["tradesCount"] ?: map["trades"], 0)
            val wins = parseInt(map["wins"] ?: map["totalWins"] ?: map["winCount"], 0)
            val losses = parseInt(map["losses"] ?: map["totalLosses"] ?: map["lossCount"], 0)
            val pnl = parseDouble(map["pnl"] ?: map["totalPnl"] ?: map["total_pnl"], 0.0)
            val createdAt = parseLong(map["createdAt"] ?: map["created_at"], System.currentTimeMillis())
            val updatedAt = parseLong(map["updatedAt"] ?: map["updated_at"], createdAt)
            val hasCompletedProfile = (map["hasCompletedProfile"] as? Boolean)
                ?: (map["has_completed_profile"] as? Boolean)
                ?: (username.isNotBlank() && username != "Trader" && !username.startsWith("user_"))

            // Clean legacy 1000 score if user actually has 0 trades
            val score = if (totalTrades == 0 && wins == 0 && losses == 0 && pnl == 0.0 && rawScore == 1000L) 0L else rawScore

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
                losses = losses,
                pnl = pnl,
                createdAt = createdAt,
                updatedAt = updatedAt,
                hasCompletedProfile = hasCompletedProfile
            )
        }
    }
}

data class LeaderboardEntry(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val photoURL: String = "",
    val score: Long = 0L,
    val totalTrades: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
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
        "wins" to wins,
        "losses" to losses,
        "winRate" to winRate,
        "totalPnl" to totalPnl,
        "updatedAt" to updatedAt
    )

    companion object {
        private fun parseLong(value: Any?, default: Long = 0L): Long {
            return when (value) {
                is Number -> value.toLong()
                is String -> value.toDoubleOrNull()?.toLong() ?: default
                else -> default
            }
        }

        private fun parseInt(value: Any?, default: Int = 0): Int {
            return when (value) {
                is Number -> value.toInt()
                is String -> value.toDoubleOrNull()?.toInt() ?: default
                else -> default
            }
        }

        private fun parseDouble(value: Any?, default: Double = 0.0): Double {
            return when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull() ?: default
                else -> default
            }
        }

        fun fromMap(uid: String, map: Map<String, Any?>): LeaderboardEntry {
            val username = ((map["username"] as? String)
                ?: (map["userName"] as? String)
                ?: (map["displayName"] as? String)
                ?: (map["name"] as? String))?.takeIf { it.isNotBlank() } ?: "Trader"
            val displayName = ((map["displayName"] as? String)
                ?: (map["display_name"] as? String)
                ?: (map["name"] as? String))?.takeIf { it.isNotBlank() } ?: username
            val photoURL = (map["photoURL"] as? String)
                ?: (map["photoUrl"] as? String)
                ?: (map["photo_url"] as? String)
                ?: (map["avatar"] as? String)
                ?: (map["avatarUrl"] as? String)
                ?: ""
            val rawScore = parseLong(map["score"] ?: map["currentScore"] ?: map["points"] ?: map["totalScore"], 0L)
            val totalTrades = parseInt(map["totalTrades"] ?: map["total_trades"] ?: map["tradesCount"] ?: map["trades"], 0)
            val wins = parseInt(map["wins"] ?: map["totalWins"] ?: map["winCount"], 0)
            val losses = parseInt(map["losses"] ?: map["totalLosses"] ?: map["lossCount"], 0)
            val settled = wins + losses
            val winRate = parseDouble(map["winRate"] ?: map["win_rate"], if (settled > 0) (wins.toDouble() / settled) * 100.0 else 0.0)
            val totalPnl = parseDouble(map["totalPnl"] ?: map["total_pnl"] ?: map["pnl"], 0.0)
            val updatedAt = parseLong(map["updatedAt"] ?: map["updated_at"] ?: map["createdAt"] ?: map["created_at"], System.currentTimeMillis())

            // Clean legacy 1000 score if user actually has 0 trades
            val score = if (totalTrades == 0 && wins == 0 && losses == 0 && totalPnl == 0.0 && rawScore == 1000L) 0L else rawScore

            return LeaderboardEntry(
                uid = uid,
                username = username,
                displayName = displayName,
                photoURL = photoURL,
                score = score,
                totalTrades = totalTrades,
                wins = wins,
                losses = losses,
                winRate = winRate,
                totalPnl = totalPnl,
                updatedAt = updatedAt
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
    val currentScore: Long = 0L,
    val rank: Int = 1,
    val totalTradersCount: Int = 1
)
