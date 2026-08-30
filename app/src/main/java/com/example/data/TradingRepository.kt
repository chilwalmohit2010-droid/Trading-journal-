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

        val authStateListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                // Fetch profile document
                val firestore = FirebaseManager.firestore
                val rtdb = FirebaseManager.database
                val uid = firebaseUser.uid
                val fallbackUsername = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "GM Trader"

                if (firestore != null) {
                    firestore.collection("users").document(uid)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            if (snapshot != null && snapshot.exists()) {
                                val profile = UserProfile.fromMap(uid, snapshot.data ?: emptyMap())
                                trySend(profile)
                            } else {
                                // Try Realtime DB if firestore doc not found
                                if (rtdb != null) {
                                    rtdb.reference.child("users").child(uid).get()
                                        .addOnSuccessListener { rtdbSnapshot ->
                                            if (rtdbSnapshot.exists()) {
                                                @Suppress("UNCHECKED_CAST")
                                                val map = rtdbSnapshot.value as? Map<String, Any?> ?: emptyMap()
                                                trySend(UserProfile.fromMap(uid, map))
                                            } else {
                                                trySend(UserProfile(uid = uid, username = fallbackUsername, email = firebaseUser.email ?: ""))
                                            }
                                        }
                                        .addOnFailureListener {
                                            trySend(UserProfile(uid = uid, username = fallbackUsername, email = firebaseUser.email ?: ""))
                                        }
                                } else {
                                    trySend(UserProfile(uid = uid, username = fallbackUsername, email = firebaseUser.email ?: ""))
                                }
                            }
                        }
                        .addOnFailureListener {
                            trySend(UserProfile(uid = uid, username = fallbackUsername, email = firebaseUser.email ?: ""))
                        }
                } else {
                    val fallbackProfile = UserProfile(
                        uid = uid,
                        username = fallbackUsername,
                        email = firebaseUser.email ?: ""
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

            val profile = UserProfile(
                uid = uid,
                username = trimmedUsername,
                email = email.trim(),
                createdAt = System.currentTimeMillis()
            )

            // Save to Firestore users collection
            FirebaseManager.firestore?.collection("users")?.document(uid)?.set(profile.toMap())?.await()

            // Save to Realtime Database users node
            FirebaseManager.database?.reference?.child("users")?.child(uid)?.setValue(profile.toMap())?.await()

            // Initialize in leaderboard
            val initialLeaderboard = LeaderboardEntry(
                uid = uid,
                username = trimmedUsername,
                score = ScoreCalculator.BASE_SCORE,
                totalTrades = 0,
                winRate = 0.0,
                totalPnl = 0.0,
                updatedAt = System.currentTimeMillis()
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
                email = user.email ?: ""
            )

            // Ensure profile exists in Firestore / RTDB
            try {
                FirebaseManager.firestore?.collection("users")?.document(uid)?.set(finalProfile.toMap(), SetOptions.merge())
                FirebaseManager.database?.reference?.child("users")?.child(uid)?.updateChildren(finalProfile.toMap())
            } catch (e: Exception) {
                Log.w(TAG, "Sync profile notice: ${e.message}")
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

        val firestore = FirebaseManager.firestore
        val rtdb = FirebaseManager.database

        var firestoreListener: ListenerRegistration? = null
        var rtdbListener: ValueEventListener? = null

        // 2. Attach Firestore Real-time listener for users/{uid}/trades
        if (firestore != null) {
            try {
                firestoreListener = firestore.collection("users").document(uid).collection("trades")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
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
                            }
                            trySend(trades)

                            // Cache in local Room DB
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

        // 3. Attach Realtime Database listener for users/{uid}/trades
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

                            // Cache in local Room DB
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
            val tradeId = if (trade.id.isEmpty()) UUID.randomUUID().toString() else trade.id
            val finalTrade = trade.copy(id = tradeId, userId = uid)

            // Save to local Room DB first for instant responsiveness
            try {
                localDb?.tradeDao()?.insertTrade(TradeEntity.fromTrade(finalTrade))
            } catch (e: Exception) {
                Log.w(TAG, "Local Room insert error: ${e.message}")
            }

            // Save under "users/{uid}/trades" in Firestore
            try {
                FirebaseManager.firestore?.collection("users")?.document(uid)
                    ?.collection("trades")?.document(tradeId)
                    ?.set(finalTrade.toMap(), SetOptions.merge())
                    ?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore trade save notice: ${e.message}")
            }

            // Save under "users/{uid}/trades" in Realtime Database
            try {
                FirebaseManager.database?.reference?.child("users")?.child(uid)
                    ?.child("trades")?.child(tradeId)
                    ?.setValue(finalTrade.toMap())
                    ?.await()
            } catch (e: Exception) {
                Log.w(TAG, "RTDB trade save notice: ${e.message}")
            }

            // Recalculate and update leaderboard score
            updateLeaderboardScore(uid, username)

            Result.success(finalTrade)
        } catch (e: Exception) {
            Log.e(TAG, "Save trade error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteTrade(uid: String, username: String, tradeId: String): Result<Unit> {
        return try {
            // Delete from local Room cache
            try {
                localDb?.tradeDao()?.deleteTradeById(tradeId)
            } catch (e: Exception) {
                Log.w(TAG, "Local Room delete error: ${e.message}")
            }

            // Delete from Firestore
            try {
                FirebaseManager.firestore?.collection("users")?.document(uid)
                    ?.collection("trades")?.document(tradeId)
                    ?.delete()
                    ?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore trade delete notice: ${e.message}")
            }

            // Delete from Realtime Database
            try {
                FirebaseManager.database?.reference?.child("users")?.child(uid)
                    ?.child("trades")?.child(tradeId)
                    ?.removeValue()
                    ?.await()
            } catch (e: Exception) {
                Log.w(TAG, "RTDB trade delete notice: ${e.message}")
            }

            // Recalculate leaderboard score
            updateLeaderboardScore(uid, username)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Delete trade error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Observe Global Leaderboard
    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> = callbackFlow {
        val firestore = FirebaseManager.firestore
        val rtdb = FirebaseManager.database

        var firestoreListener: ListenerRegistration? = null
        var rtdbListener: ValueEventListener? = null

        if (firestore != null) {
            try {
                firestoreListener = firestore.collection("leaderboard")
                    .orderBy("score", Query.Direction.DESCENDING)
                    .limit(100)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w(TAG, "Leaderboard Firestore listen error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val entries = snapshot.documents.mapNotNull { doc ->
                                try {
                                    LeaderboardEntry.fromMap(doc.id, doc.data ?: emptyMap())
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            trySend(entries)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Leaderboard Firestore setup error: ${e.message}", e)
            }
        }

        if (rtdb != null) {
            try {
                val lbRef = rtdb.reference.child("leaderboard")
                rtdbListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val entries = mutableListOf<LeaderboardEntry>()
                            for (child in snapshot.children) {
                                try {
                                    @Suppress("UNCHECKED_CAST")
                                    val map = child.value as? Map<String, Any?>
                                    if (map != null) {
                                        entries.add(LeaderboardEntry.fromMap(child.key ?: "", map))
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Leaderboard RTDB parse error: ${e.message}")
                                }
                            }
                            val sorted = entries.sortedByDescending { it.score }
                            trySend(sorted)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "RTDB leaderboard cancelled: ${error.message}")
                    }
                }
                lbRef.addValueEventListener(rtdbListener)
            } catch (e: Exception) {
                Log.e(TAG, "RTDB leaderboard setup error: ${e.message}", e)
            }
        }

        awaitClose {
            firestoreListener?.remove()
            if (rtdbListener != null && rtdb != null) {
                try {
                    rtdb.reference.child("leaderboard").removeEventListener(rtdbListener)
                } catch (e: Exception) {
                    Log.w(TAG, "Error removing RTDB leaderboard listener: ${e.message}")
                }
            }
        }
    }

    private suspend fun updateLeaderboardScore(uid: String, username: String) {
        try {
            // Fetch latest user trades from Firestore / RTDB
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

            val calculatedScore = ScoreCalculator.calculateScore(allTrades)
            val wins = allTrades.count { it.result == TradeResult.WIN }
            val losses = allTrades.count { it.result == TradeResult.LOSS }
            val settled = wins + losses
            val winRate = if (settled > 0) (wins.toDouble() / settled.toDouble()) * 100.0 else 0.0
            val totalPnl = allTrades.sumOf { it.pnl }

            val entry = LeaderboardEntry(
                uid = uid,
                username = username,
                displayName = username,
                score = calculatedScore,
                totalTrades = allTrades.size,
                winRate = winRate,
                totalPnl = totalPnl,
                updatedAt = System.currentTimeMillis()
            )

            FirebaseManager.firestore?.collection("leaderboard")?.document(uid)?.set(entry.toMap(), SetOptions.merge())
            FirebaseManager.database?.reference?.child("leaderboard")?.child(uid)?.setValue(entry.toMap())
        } catch (e: Exception) {
            Log.w(TAG, "Could not update leaderboard score: ${e.message}")
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

            val updates = mapOf<String, Any?>(
                "username" to cleanUsername,
                "displayName" to cleanDisplayName,
                "bio" to cleanBio,
                "photoURL" to photoURL
            )

            // Update Firestore users/{uid}
            FirebaseManager.firestore?.collection("users")?.document(uid)?.set(updates, SetOptions.merge())?.await()

            // Update Realtime Database
            FirebaseManager.database?.reference?.child("users")?.child(uid)?.updateChildren(updates)?.await()

            // Update Leaderboard entry if exists
            val leaderboardUpdates = mapOf<String, Any?>(
                "username" to cleanUsername,
                "displayName" to cleanDisplayName,
                "photoURL" to photoURL
            )
            FirebaseManager.firestore?.collection("leaderboard")?.document(uid)?.set(leaderboardUpdates, SetOptions.merge())
            FirebaseManager.database?.reference?.child("leaderboard")?.child(uid)?.updateChildren(leaderboardUpdates)

            // Fetch updated profile
            val currentDoc = FirebaseManager.firestore?.collection("users")?.document(uid)?.get()?.await()
            val updatedProfile = if (currentDoc != null && currentDoc.exists()) {
                UserProfile.fromMap(uid, currentDoc.data ?: emptyMap())
            } else {
                UserProfile(
                    uid = uid,
                    username = cleanUsername,
                    displayName = cleanDisplayName,
                    bio = cleanBio,
                    photoURL = photoURL,
                    email = FirebaseManager.auth?.currentUser?.email ?: ""
                )
            }

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

