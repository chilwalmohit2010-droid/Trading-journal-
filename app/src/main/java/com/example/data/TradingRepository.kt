package com.example.data

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.TradeEntity
import com.example.data.model.LeaderboardEntry
import com.example.data.model.Trade
import com.example.data.model.TradeResult
import com.example.data.model.UserProfile
import com.example.domain.ScoreCalculator
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TradingRepository {
    private val TAG = "TradingRepository"

    private val localDb: AppDatabase?
        get() = FirebaseManager.context?.let { AppDatabase.getInstance(it) }

    // Real-time auth state flow
    fun observeAuthState(): Flow<UserProfile?> = callbackFlow {
        val auth = FirebaseManager.auth
        if (auth == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        var firestoreUserListener: ListenerRegistration? = null

        val authStateListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            firestoreUserListener?.remove()
            firestoreUserListener = null

            if (firebaseUser != null) {
                val firestore = FirebaseManager.firestore
                val rtdb = FirebaseManager.database
                val uid = firebaseUser.uid
                val fallbackUsername = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "GM Trader"

                // Real-time Snapshot Listener on users/{uid} (Read-only, no write-backs)
                if (firestore != null) {
                    try {
                        firestoreUserListener = firestore.collection("users").document(uid)
                            .addSnapshotListener { snapshot, error ->
                                if (error != null) {
                                    Log.w(TAG, "Firestore user listener error: ${error.message}")
                                    return@addSnapshotListener
                                }
                                if (snapshot != null && snapshot.exists()) {
                                    val profile = UserProfile.fromMap(uid, snapshot.data ?: emptyMap())
                                    trySend(profile)
                                } else {
                                    // If doc does not exist yet in Firestore, try RTDB or fallback
                                    if (rtdb != null) {
                                        rtdb.reference.child("users").child(uid).get()
                                            .addOnSuccessListener { rtdbSnapshot ->
                                                if (rtdbSnapshot.exists()) {
                                                    @Suppress("UNCHECKED_CAST")
                                                    val map = rtdbSnapshot.value as? Map<String, Any?> ?: emptyMap()
                                                    val profile = UserProfile.fromMap(uid, map)
                                                    trySend(profile)
                                                } else {
                                                    val fallbackProfile = UserProfile(
                                                        uid = uid,
                                                        username = fallbackUsername,
                                                        displayName = fallbackUsername,
                                                        email = firebaseUser.email ?: "",
                                                        hasCompletedProfile = false
                                                    )
                                                    trySend(fallbackProfile)
                                                }
                                            }
                                    } else {
                                        val fallbackProfile = UserProfile(
                                            uid = uid,
                                            username = fallbackUsername,
                                            displayName = fallbackUsername,
                                            email = firebaseUser.email ?: "",
                                            hasCompletedProfile = false
                                        )
                                        trySend(fallbackProfile)
                                    }
                                }
                            }
                    } catch (e: Exception) {
                        Log.w(TAG, "User snapshot listener setup error: ${e.message}")
                    }
                } else {
                    val fallbackProfile = UserProfile(
                        uid = uid,
                        username = fallbackUsername,
                        displayName = fallbackUsername,
                        email = firebaseUser.email ?: "",
                        hasCompletedProfile = false
                    )
                    trySend(fallbackProfile)
                }
            } else {
                trySend(null)
            }
        }

        auth.addAuthStateListener(authStateListener)
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
            firestoreUserListener?.remove()
        }
    }

    suspend fun signUp(username: String, email: String, password: String): Result<UserProfile> {
        val auth = FirebaseManager.auth ?: return Result.failure(Exception("Firebase Authentication service unavailable"))

        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user ?: throw Exception("Failed to create account")
            val uid = user.uid
            val trimmedUsername = username.trim().ifEmpty { email.substringBefore("@") }

            try {
                user.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(trimmedUsername)
                        .build()
                ).await()
            } catch (e: Exception) {
                Log.w(TAG, "Notice setting displayName: ${e.message}")
            }

            val now = System.currentTimeMillis()
            val profile = UserProfile(
                uid = uid,
                username = trimmedUsername,
                displayName = trimmedUsername,
                bio = "",
                photoURL = "",
                email = email.trim(),
                score = ScoreCalculator.BASE_SCORE,
                totalTrades = 0,
                wins = 0,
                losses = 0,
                pnl = 0.0,
                createdAt = now,
                updatedAt = now,
                hasCompletedProfile = false
            )

            // Save to Firestore users collection
            FirebaseManager.firestore?.collection("users")?.document(uid)?.set(profile.toMap())?.await()

            // Save to Realtime Database users node
            FirebaseManager.database?.reference?.child("users")?.child(uid)?.setValue(profile.toMap())?.await()

            // Initialize in leaderboard
            val initialLeaderboard = LeaderboardEntry(
                uid = uid,
                username = trimmedUsername,
                displayName = trimmedUsername,
                photoURL = "",
                score = ScoreCalculator.BASE_SCORE,
                totalTrades = 0,
                wins = 0,
                losses = 0,
                winRate = 0.0,
                totalPnl = 0.0,
                updatedAt = now
            )
            FirebaseManager.firestore?.collection("leaderboard")?.document(uid)?.set(initialLeaderboard.toMap())?.await()
            FirebaseManager.database?.reference?.child("leaderboard")?.child(uid)?.setValue(initialLeaderboard.toMap())?.await()

            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<UserProfile> {
        val auth = FirebaseManager.auth ?: return Result.failure(Exception("Firebase Authentication service unavailable"))

        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user ?: throw Exception("User not found")
            val uid = user.uid

            var profile: UserProfile? = null

            // Try loading from Firestore
            try {
                val snapshot = FirebaseManager.firestore?.collection("users")?.document(uid)?.get()?.await()
                if (snapshot != null && snapshot.exists()) {
                    profile = UserProfile.fromMap(uid, snapshot.data ?: emptyMap())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore user fetch notice: ${e.message}")
            }

            // If not found, try Realtime DB
            if (profile == null) {
                try {
                    val rtdbSnapshot = FirebaseManager.database?.reference?.child("users")?.child(uid)?.get()?.await()
                    if (rtdbSnapshot != null && rtdbSnapshot.exists()) {
                        @Suppress("UNCHECKED_CAST")
                        val map = rtdbSnapshot.value as? Map<String, Any?> ?: emptyMap()
                        profile = UserProfile.fromMap(uid, map)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "RTDB user fetch notice: ${e.message}")
                }
            }

            val finalProfile = profile ?: UserProfile(
                uid = uid,
                username = user.displayName ?: user.email?.substringBefore("@") ?: "GM Trader",
                displayName = user.displayName ?: user.email?.substringBefore("@") ?: "GM Trader",
                email = user.email ?: "",
                score = ScoreCalculator.BASE_SCORE,
                totalTrades = 0,
                wins = 0,
                losses = 0,
                pnl = 0.0,
                hasCompletedProfile = false
            )

            // If profile didn't exist yet, initialize it in Firestore / RTDB
            if (profile == null) {
                try {
                    FirebaseManager.firestore?.collection("users")?.document(uid)?.set(finalProfile.toMap(), SetOptions.merge())
                    FirebaseManager.database?.reference?.child("users")?.child(uid)?.updateChildren(finalProfile.toMap())
                } catch (e: Exception) {
                    Log.w(TAG, "Sync initial profile notice: ${e.message}")
                }
            }

            Result.success(finalProfile)
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun logout() {
        try {
            FirebaseManager.auth?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Logout error: ${e.message}", e)
        }
    }

    suspend fun fetchAllUserTradesDirect(uid: String): List<Trade> {
        if (uid.isBlank()) return emptyList()
        val tradesMap = mutableMapOf<String, Trade>()

        // 1. Try Firestore users/{uid}/trades
        try {
            val snapshot = FirebaseManager.firestore?.collection("users")?.document(uid)?.collection("trades")?.get()?.await()
            if (snapshot != null && !snapshot.isEmpty) {
                snapshot.documents.forEach { doc ->
                    try {
                        val trade = Trade.fromMap(doc.id, doc.data ?: emptyMap())
                        tradesMap[trade.id.ifEmpty { doc.id }] = trade
                    } catch (e: Exception) {
                        Log.w(TAG, "Parse user trade error: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch firestore subcollection trades notice: ${e.message}")
        }

        // 2. Try Firestore root trades collection (fallback)
        try {
            val rootSnapshot = FirebaseManager.firestore?.collection("trades")
                ?.whereEqualTo("userId", uid)?.get()?.await()
            if (rootSnapshot != null && !rootSnapshot.isEmpty) {
                rootSnapshot.documents.forEach { doc ->
                    try {
                        val trade = Trade.fromMap(doc.id, doc.data ?: emptyMap())
                        tradesMap[trade.id.ifEmpty { doc.id }] = trade
                    } catch (e: Exception) {
                        Log.w(TAG, "Parse root trade error: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch firestore root trades notice: ${e.message}")
        }

        // 3. Try Realtime Database
        try {
            val rtdbSnapshot = FirebaseManager.database?.reference?.child("users")?.child(uid)?.child("trades")?.get()?.await()
            if (rtdbSnapshot != null && rtdbSnapshot.exists()) {
                for (child in rtdbSnapshot.children) {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val map = child.value as? Map<String, Any?>
                        if (map != null) {
                            val id = child.key ?: ""
                            val trade = Trade.fromMap(id, map)
                            tradesMap[trade.id.ifEmpty { id }] = trade
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Parse RTDB trade child error: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch RTDB trades notice: ${e.message}")
        }

        // 4. Try local Room DB
        try {
            localDb?.tradeDao()?.getTradesForUserOnce(uid)?.let { cached ->
                cached.forEach { entity ->
                    val trade = entity.toTrade()
                    if (!tradesMap.containsKey(trade.id)) {
                        tradesMap[trade.id] = trade
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch local DB trades notice: ${e.message}")
        }

        return tradesMap.values.sortedByDescending { it.timestamp }
    }

    // Observe real-time trades under "users/{uid}/trades" with local Room cache fallback
    fun observeUserTrades(uid: String): Flow<List<Trade>> = callbackFlow {
        if (uid.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // 1. Emit existing local cache first for instant UI response
        localDb?.let { db ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val cached = db.tradeDao().getTradesForUser(uid)
                    cached.collect { entityList ->
                        if (entityList.isNotEmpty()) {
                            trySend(entityList.map { it.toTrade() })
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Local cache read notice: ${e.message}")
                }
            }
        }

        // 2. Fetch directly to synchronize read state immediately without write-back
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val directTrades = fetchAllUserTradesDirect(uid)
                if (directTrades.isNotEmpty()) {
                    trySend(directTrades)
                    localDb?.tradeDao()?.insertTrades(directTrades.map { TradeEntity.fromTrade(it) })
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct trade fetch notice: ${e.message}")
            }
        }

        val firestore = FirebaseManager.firestore
        val rtdb = FirebaseManager.database

        var firestoreListener: ListenerRegistration? = null
        var rtdbListener: ValueEventListener? = null

        // 3. Attach Firestore Real-time listener for users/{uid}/trades (Read-only, no write-backs)
        if (firestore != null) {
            try {
                firestoreListener = firestore.collection("users").document(uid).collection("trades")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w(TAG, "Firestore trades snapshot error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val trades = snapshot.documents.mapNotNull { doc ->
                                try {
                                    Trade.fromMap(doc.id, doc.data ?: emptyMap())
                                } catch (e: Exception) {
                                    null
                                }
                            }.sortedByDescending { it.timestamp }

                            trySend(trades)

                            // Cache in local Room DB for offline reading only
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    localDb?.tradeDao()?.insertTrades(trades.map { TradeEntity.fromTrade(it) })
                                } catch (e: Exception) {
                                    Log.w(TAG, "Room cache write error: ${e.message}")
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up Firestore trades listener: ${e.message}", e)
            }
        }

        // 4. Attach Realtime Database listener for users/{uid}/trades (Read-only, no write-backs)
        if (rtdb != null) {
            try {
                val tradesRef = rtdb.reference.child("users").child(uid).child("trades")
                rtdbListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val trades = mutableListOf<Trade>()
                            for (child in snapshot.children) {
                                try {
                                    @Suppress("UNCHECKED_CAST")
                                    val map = child.value as? Map<String, Any?>
                                    if (map != null) {
                                        trades.add(Trade.fromMap(child.key ?: "", map))
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Parse RTDB trade error: ${e.message}")
                                }
                            }
                            val sortedTrades = trades.sortedByDescending { it.timestamp }
                            trySend(sortedTrades)

                            // Cache in local Room DB for offline reading only
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    localDb?.tradeDao()?.insertTrades(sortedTrades.map { TradeEntity.fromTrade(it) })
                                } catch (e: Exception) {
                                    Log.w(TAG, "Room cache write error: ${e.message}")
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "RTDB trades listener cancelled: ${error.message}")
                    }
                }
                tradesRef.addValueEventListener(rtdbListener)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up RTDB trades listener: ${e.message}", e)
            }
        }

        awaitClose {
            firestoreListener?.remove()
            if (rtdbListener != null && rtdb != null) {
                try {
                    rtdb.reference.child("users").child(uid).child("trades").removeEventListener(rtdbListener)
                } catch (e: Exception) {
                    Log.w(TAG, "Error removing RTDB listener: ${e.message}")
                }
            }
        }
    }

    suspend fun saveTrade(uid: String, username: String, trade: Trade): Result<Trade> {
        return try {
            val tradeId = if (trade.id.isNotBlank()) trade.id else UUID.randomUUID().toString()
            val finalTrade = trade.copy(id = tradeId, userId = uid)

            val firestore = FirebaseManager.firestore
            val rtdb = FirebaseManager.database

            // 1. Write the trade to Firestore FIRST. If this fails, fail immediately without touching stats.
            if (firestore != null) {
                firestore.collection("users").document(uid)
                    .collection("trades").document(tradeId)
                    .set(finalTrade.toMap(), SetOptions.merge())
                    .await()
            }

            // 2. Also write to Room DB and Realtime Database for consistency
            try {
                localDb?.tradeDao()?.insertTrade(TradeEntity.fromTrade(finalTrade))
            } catch (e: Exception) {
                Log.w(TAG, "Local Room insert notice: ${e.message}")
            }

            if (rtdb != null) {
                try {
                    rtdb.reference.child("users").child(uid)
                        .child("trades").child(tradeId)
                        .setValue(finalTrade.toMap())
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "RTDB trade save notice: ${e.message}")
                }
            }

            // 3. Only after successful Firestore write, recalculate user statistics exactly once
            val allTrades = fetchAllUserTradesDirect(uid)
            syncUserTradesAndScore(uid, allTrades)

            Result.success(finalTrade)
        } catch (e: Exception) {
            Log.e(TAG, "Save trade error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteTrade(uid: String, username: String, tradeId: String): Result<Unit> {
        return try {
            val firestore = FirebaseManager.firestore
            val rtdb = FirebaseManager.database

            // 1. Delete from Firestore first
            if (firestore != null) {
                firestore.collection("users").document(uid)
                    .collection("trades").document(tradeId)
                    .delete()
                    .await()
            }

            // 2. Delete from local Room cache & Realtime Database
            try {
                localDb?.tradeDao()?.deleteTradeById(tradeId)
            } catch (e: Exception) {
                Log.w(TAG, "Local Room delete notice: ${e.message}")
            }

            if (rtdb != null) {
                try {
                    rtdb.reference.child("users").child(uid)
                        .child("trades").child(tradeId)
                        .removeValue()
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "RTDB trade delete notice: ${e.message}")
                }
            }

            // 3. Recalculate leaderboard score once
            val allTrades = fetchAllUserTradesDirect(uid)
            syncUserTradesAndScore(uid, allTrades)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Delete trade error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Observe Global Leaderboard with real-time updates across all registered users in Firestore
    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> = callbackFlow {
        val firestore = FirebaseManager.firestore
        val rtdb = FirebaseManager.database

        val allUsersMap = java.util.concurrent.ConcurrentHashMap<String, LeaderboardEntry>()

        var firestoreUsersListener: ListenerRegistration? = null
        var firestoreLeaderboardListener: ListenerRegistration? = null
        var rtdbUsersListener: ValueEventListener? = null
        var rtdbLeaderboardListener: ValueEventListener? = null

        // Helper to merge, sort, and rank all known users across data sources
        fun mergeAndEmit(entries: List<LeaderboardEntry>) {
            if (entries.isEmpty() && allUsersMap.isNotEmpty()) {
                return
            }
            for (entry in entries) {
                if (entry.uid.isBlank()) continue
                allUsersMap.compute(entry.uid) { _, existing ->
                    if (existing == null) {
                        entry
                    } else {
                        val username = if (entry.username.isNotBlank() && entry.username != "Trader") entry.username else existing.username
                        val displayName = if (entry.displayName.isNotBlank() && entry.displayName != "Trader") entry.displayName else existing.displayName
                        val photoURL = if (entry.photoURL.isNotBlank()) entry.photoURL else existing.photoURL
                        val isNewer = entry.updatedAt >= existing.updatedAt
                        val score = if (isNewer) entry.score else existing.score
                        val totalTrades = if (isNewer) entry.totalTrades else existing.totalTrades
                        val wins = if (isNewer) entry.wins else existing.wins
                        val losses = if (isNewer) entry.losses else existing.losses
                        val winRate = if (isNewer) entry.winRate else existing.winRate
                        val totalPnl = if (isNewer) entry.totalPnl else existing.totalPnl
                        val updatedAt = maxOf(entry.updatedAt, existing.updatedAt)

                        LeaderboardEntry(
                            uid = entry.uid,
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

            val sorted = allUsersMap.values
                .sortedWith(
                    compareByDescending<LeaderboardEntry> { it.score }
                        .thenByDescending { it.totalTrades }
                        .thenByDescending { it.winRate }
                        .thenByDescending { it.updatedAt }
                        .thenBy { it.username.lowercase() }
                )
            for ((idx, item) in sorted.withIndex()) {
                Log.d("LEADERBOARD_DEBUG", "LEADERBOARD DATA: username=${item.username}, score=${item.score}, totalTrades=${item.totalTrades}, rank=${idx + 1}")
            }
            trySend(sorted)
        }

        // 1. Initial direct fetch of the complete 'users' and 'leaderboard' collections without filtering
        if (firestore != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val snapshot = firestore.collection("users").get().await()
                    if (snapshot != null && !snapshot.isEmpty) {
                        val entries = snapshot.documents.mapNotNull { doc ->
                            try {
                                val data = doc.data ?: return@mapNotNull null
                                val uid = doc.id
                                if (uid.isBlank()) return@mapNotNull null
                                LeaderboardEntry.fromMap(uid, data)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        mergeAndEmit(entries)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Initial users direct fetch notice: ${e.message}")
                }

                try {
                    val snapshot = firestore.collection("leaderboard").get().await()
                    if (snapshot != null && !snapshot.isEmpty) {
                        val entries = snapshot.documents.mapNotNull { doc ->
                            try {
                                val data = doc.data ?: return@mapNotNull null
                                val uid = doc.id
                                if (uid.isBlank()) return@mapNotNull null
                                LeaderboardEntry.fromMap(uid, data)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        mergeAndEmit(entries)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Initial leaderboard direct fetch notice: ${e.message}")
                }
            }

            // 2. Real-time snapshot listener on the complete 'users' collection
            try {
                firestoreUsersListener = firestore.collection("users")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w(TAG, "Leaderboard Firestore users listen error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val entries = snapshot.documents.mapNotNull { doc ->
                                try {
                                    val data = doc.data ?: return@mapNotNull null
                                    val uid = doc.id
                                    if (uid.isBlank()) return@mapNotNull null
                                    LeaderboardEntry.fromMap(uid, data)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            mergeAndEmit(entries)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Leaderboard Firestore users setup error: ${e.message}", e)
            }

            // 3. Real-time snapshot listener on the 'leaderboard' collection
            try {
                firestoreLeaderboardListener = firestore.collection("leaderboard")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w(TAG, "Leaderboard Firestore leaderboard listen error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val entries = snapshot.documents.mapNotNull { doc ->
                                try {
                                    val data = doc.data ?: return@mapNotNull null
                                    val uid = doc.id
                                    if (uid.isBlank()) return@mapNotNull null
                                    LeaderboardEntry.fromMap(uid, data)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            mergeAndEmit(entries)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Leaderboard Firestore leaderboard setup error: ${e.message}", e)
            }
        }

        // 4. Realtime Database listeners (merging into allUsersMap)
        if (rtdb != null) {
            try {
                val usersRef = rtdb.reference.child("users")
                rtdbUsersListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val entries = mutableListOf<LeaderboardEntry>()
                            for (child in snapshot.children) {
                                try {
                                    @Suppress("UNCHECKED_CAST")
                                    val map = child.value as? Map<String, Any?>
                                    if (map != null) {
                                        val uid = child.key ?: ""
                                        if (uid.isNotBlank()) {
                                            entries.add(LeaderboardEntry.fromMap(uid, map))
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Leaderboard RTDB users parse error: ${e.message}")
                                }
                            }
                            mergeAndEmit(entries)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "RTDB users leaderboard cancelled: ${error.message}")
                    }
                }
                usersRef.addValueEventListener(rtdbUsersListener)
            } catch (e: Exception) {
                Log.e(TAG, "RTDB users leaderboard setup error: ${e.message}", e)
            }

            try {
                val leaderboardRef = rtdb.reference.child("leaderboard")
                rtdbLeaderboardListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val entries = mutableListOf<LeaderboardEntry>()
                            for (child in snapshot.children) {
                                try {
                                    @Suppress("UNCHECKED_CAST")
                                    val map = child.value as? Map<String, Any?>
                                    if (map != null) {
                                        val uid = child.key ?: ""
                                        if (uid.isNotBlank()) {
                                            entries.add(LeaderboardEntry.fromMap(uid, map))
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Leaderboard RTDB parse error: ${e.message}")
                                }
                            }
                            mergeAndEmit(entries)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "RTDB leaderboard cancelled: ${error.message}")
                    }
                }
                leaderboardRef.addValueEventListener(rtdbLeaderboardListener)
            } catch (e: Exception) {
                Log.e(TAG, "RTDB leaderboard setup error: ${e.message}", e)
            }
        }

        awaitClose {
            firestoreUsersListener?.remove()
            firestoreLeaderboardListener?.remove()
            if (rtdbUsersListener != null && rtdb != null) {
                try {
                    rtdb.reference.child("users").removeEventListener(rtdbUsersListener)
                } catch (e: Exception) {
                    Log.w(TAG, "Error removing RTDB users listener: ${e.message}")
                }
            }
            if (rtdbLeaderboardListener != null && rtdb != null) {
                try {
                    rtdb.reference.child("leaderboard").removeEventListener(rtdbLeaderboardListener)
                } catch (e: Exception) {
                    Log.w(TAG, "Error removing RTDB leaderboard listener: ${e.message}")
                }
            }
        }
    }

    suspend fun syncUserTradesAndScore(uid: String, trades: List<Trade>) {
        try {
            if (uid.isBlank()) return
            val calculatedScore = ScoreCalculator.calculateScore(trades)
            val wins = trades.count { it.result == TradeResult.WIN }
            val losses = trades.count { it.result == TradeResult.LOSS }
            val settled = wins + losses
            val winRate = if (settled > 0) (wins.toDouble() / settled.toDouble()) * 100.0 else 0.0
            val totalPnl = trades.sumOf { it.pnl }
            val now = System.currentTimeMillis()

            // Fetch latest user doc to preserve displayName and photoURL
            val userDoc = FirebaseManager.firestore?.collection("users")?.document(uid)?.get()?.await()
            val userData = userDoc?.data ?: emptyMap()
            val currentUsername = (userData["username"] as? String)?.ifBlank { null }
                ?: FirebaseManager.auth?.currentUser?.displayName
                ?: FirebaseManager.auth?.currentUser?.email?.substringBefore("@")
                ?: "Trader"
            val currentDisplayName = (userData["displayName"] as? String)?.ifBlank { currentUsername } ?: currentUsername
            val currentPhotoURL = (userData["photoURL"] as? String) ?: (userData["photoUrl"] as? String ?: "")
            val bio = (userData["bio"] as? String) ?: ""
            val email = (userData["email"] as? String) ?: (FirebaseManager.auth?.currentUser?.email ?: "")
            val createdAt = (userData["createdAt"] as? Number)?.toLong() ?: now
            val hasCompletedProfile = (userData["hasCompletedProfile"] as? Boolean) ?: true

            val scoreUpdates = mapOf<String, Any?>(
                "uid" to uid,
                "username" to currentUsername,
                "displayName" to currentDisplayName,
                "bio" to bio,
                "photoURL" to currentPhotoURL,
                "email" to email,
                "score" to calculatedScore,
                "totalTrades" to trades.size,
                "wins" to wins,
                "losses" to losses,
                "winRate" to winRate,
                "pnl" to totalPnl,
                "totalPnl" to totalPnl,
                "hasCompletedProfile" to hasCompletedProfile,
                "createdAt" to createdAt,
                "updatedAt" to now
            )

            Log.d("TRADING_SYNC", """
AUTH UID: $uid
USER DOCUMENT: users/$uid
USER SCORE: $calculatedScore
USER TOTAL TRADES: ${trades.size}
TRADE DOCUMENT COUNT: ${trades.size}
""".trimIndent())

            // Update Firestore users/{uid} document as the single source of truth
            FirebaseManager.firestore?.collection("users")?.document(uid)?.set(scoreUpdates, SetOptions.merge())?.await()

            // Update RTDB users/{uid} node
            FirebaseManager.database?.reference?.child("users")?.child(uid)?.updateChildren(scoreUpdates)?.await()

            // Also keep leaderboard node synced
            val entry = LeaderboardEntry(
                uid = uid,
                username = currentUsername,
                displayName = currentDisplayName,
                photoURL = currentPhotoURL,
                score = calculatedScore,
                totalTrades = trades.size,
                wins = wins,
                losses = losses,
                winRate = winRate,
                totalPnl = totalPnl,
                updatedAt = now
            )
            FirebaseManager.firestore?.collection("leaderboard")?.document(uid)?.set(entry.toMap(), SetOptions.merge())
            FirebaseManager.database?.reference?.child("leaderboard")?.child(uid)?.setValue(entry.toMap())
        } catch (e: Exception) {
            Log.w(TAG, "Could not sync user trades and score: ${e.message}")
        }
    }

    private suspend fun updateLeaderboardScore(uid: String, username: String) {
        try {
            // Fetch latest user trades from Firestore / RTDB / Local DB
            val allTrades = mutableListOf<Trade>()

            try {
                val tradesSnapshot = FirebaseManager.firestore?.collection("users")?.document(uid)?.collection("trades")?.get()?.await()
                if (tradesSnapshot != null && !tradesSnapshot.isEmpty) {
                    allTrades.addAll(tradesSnapshot.documents.mapNotNull { doc ->
                        try { Trade.fromMap(doc.id, doc.data ?: emptyMap()) } catch (e: Exception) { null }
                    })
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore trades fetch for score notice: ${e.message}")
            }

            if (allTrades.isEmpty()) {
                try {
                    val rtdbSnapshot = FirebaseManager.database?.reference?.child("users")?.child(uid)?.child("trades")?.get()?.await()
                    if (rtdbSnapshot != null && rtdbSnapshot.exists()) {
                        for (child in rtdbSnapshot.children) {
                            @Suppress("UNCHECKED_CAST")
                            val map = child.value as? Map<String, Any?>
                            if (map != null) {
                                allTrades.add(Trade.fromMap(child.key ?: "", map))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "RTDB trades fetch for score notice: ${e.message}")
                }
            }

            syncUserTradesAndScore(uid, allTrades)
        } catch (e: Exception) {
            Log.w(TAG, "Could not update leaderboard score: ${e.message}")
        }
    }

    suspend fun fetchUserProfile(uid: String): Result<UserProfile?> {
        if (uid.isBlank()) return Result.success(null)
        return try {
            val snapshot = FirebaseManager.firestore?.collection("users")?.document(uid)?.get()?.await()
            if (snapshot != null && snapshot.exists()) {
                val profile = UserProfile.fromMap(uid, snapshot.data ?: emptyMap())
                Result.success(profile)
            } else {
                val rtdbSnapshot = FirebaseManager.database?.reference?.child("users")?.child(uid)?.get()?.await()
                if (rtdbSnapshot != null && rtdbSnapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val map = rtdbSnapshot.value as? Map<String, Any?> ?: emptyMap()
                    Result.success(UserProfile.fromMap(uid, map))
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch user profile error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun isUsernameAvailable(username: String, currentUid: String): Boolean {
        val clean = username.trim()
        if (clean.length < 3) return false
        val firestore = FirebaseManager.firestore
        if (firestore != null) {
            try {
                val query = firestore.collection("users")
                    .whereEqualTo("username", clean)
                    .get().await()
                for (doc in query.documents) {
                    if (doc.id != currentUid) {
                        return false
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Username check notice: ${e.message}")
            }
        }
        return true
    }

    suspend fun updateProfile(
        uid: String,
        username: String,
        displayName: String,
        bio: String,
        photoURL: String
    ): Result<UserProfile> {
        return try {
            val cleanUsername = username.trim()
            val cleanDisplayName = displayName.trim().ifEmpty { cleanUsername }
            val cleanBio = bio.trim()
            val now = System.currentTimeMillis()

            // Update Auth User displayName
            FirebaseManager.auth?.currentUser?.let { user ->
                try {
                    user.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(cleanDisplayName)
                            .build()
                    ).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Update auth displayName notice: ${e.message}")
                }
            }

            // Fetch existing data to maintain score and stats
            val currentDoc = FirebaseManager.firestore?.collection("users")?.document(uid)?.get()?.await()
            val existingData = currentDoc?.data ?: emptyMap()
            val rawScore = (existingData["score"] as? Number)?.toLong() ?: 0L
            val existingTrades = (existingData["totalTrades"] as? Number)?.toInt() ?: 0
            val existingWins = (existingData["wins"] as? Number)?.toInt() ?: 0
            val existingLosses = (existingData["losses"] as? Number)?.toInt() ?: 0
            val existingPnl = (existingData["pnl"] as? Number)?.toDouble() ?: 0.0
            val existingWinRate = (existingData["winRate"] as? Number)?.toDouble()
                ?: (if (existingWins + existingLosses > 0) (existingWins.toDouble() / (existingWins + existingLosses)) * 100.0 else 0.0)
            val createdAt = (existingData["createdAt"] as? Number)?.toLong() ?: now
            val userEmail = FirebaseManager.auth?.currentUser?.email ?: (existingData["email"] as? String ?: "")

            // Sanitize legacy score if 0 trades
            val existingScore = if (existingTrades == 0 && existingWins == 0 && existingLosses == 0 && existingPnl == 0.0 && rawScore == 1000L) 0L else rawScore

            val fullUserUpdates = mapOf<String, Any?>(
                "uid" to uid,
                "username" to cleanUsername,
                "displayName" to cleanDisplayName,
                "bio" to cleanBio,
                "photoURL" to photoURL,
                "email" to userEmail,
                "score" to existingScore,
                "totalTrades" to existingTrades,
                "wins" to existingWins,
                "losses" to existingLosses,
                "winRate" to existingWinRate,
                "pnl" to existingPnl,
                "hasCompletedProfile" to true,
                "createdAt" to createdAt,
                "updatedAt" to now
            )

            // Update Firestore users/{uid}
            FirebaseManager.firestore?.collection("users")?.document(uid)?.set(fullUserUpdates, SetOptions.merge())?.await()

            // Update Realtime Database
            FirebaseManager.database?.reference?.child("users")?.child(uid)?.updateChildren(fullUserUpdates)?.await()

            // Update Leaderboard collection
            val leaderboardUpdates = LeaderboardEntry(
                uid = uid,
                username = cleanUsername,
                displayName = cleanDisplayName,
                photoURL = photoURL,
                score = existingScore,
                totalTrades = existingTrades,
                wins = existingWins,
                losses = existingLosses,
                winRate = existingWinRate,
                totalPnl = existingPnl,
                updatedAt = now
            )
            FirebaseManager.firestore?.collection("leaderboard")?.document(uid)?.set(leaderboardUpdates.toMap(), SetOptions.merge())?.await()
            FirebaseManager.database?.reference?.child("leaderboard")?.child(uid)?.setValue(leaderboardUpdates.toMap())?.await()

            val updatedProfile = UserProfile(
                uid = uid,
                username = cleanUsername,
                displayName = cleanDisplayName,
                bio = cleanBio,
                photoURL = photoURL,
                email = userEmail,
                score = existingScore,
                totalTrades = existingTrades,
                wins = existingWins,
                losses = existingLosses,
                pnl = existingPnl,
                hasCompletedProfile = true,
                createdAt = createdAt,
                updatedAt = now
            )

            Result.success(updatedProfile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadProfilePhoto(uid: String, imageBytes: ByteArray): Result<String> {
        return try {
            val storage = FirebaseManager.storage
            if (storage != null) {
                try {
                    val ref = storage.reference.child("users").child(uid).child("profile.jpg")
                    ref.putBytes(imageBytes).await()
                    val downloadUrl = ref.downloadUrl.await().toString()
                    return Result.success(downloadUrl)
                } catch (e: Exception) {
                    Log.w(TAG, "Storage upload notice, falling back to base64: ${e.message}")
                }
            }
            // Fallback: encode as compressed base64 URI
            val base64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
            Result.success("data:image/jpeg;base64,$base64")
        } catch (e: Exception) {
            Log.e(TAG, "Image upload error: ${e.message}", e)
            Result.failure(e)
        }
    }
}

