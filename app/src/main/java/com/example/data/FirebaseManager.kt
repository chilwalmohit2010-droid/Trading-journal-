package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    
    // Explicit Firebase configuration provided
    const val API_KEY = "AIzaSyAnN5qq_VEEi0copr6dM3FjKGlkxIbfse4"
    const val AUTH_DOMAIN = "trading-diary-gm.firebaseapp.com"
    const val PROJECT_ID = "trading-diary-gm"
    const val STORAGE_BUCKET = "trading-diary-gm.firebasestorage.app"
    const val MESSAGING_SENDER_ID = "513541784733"
    const val APP_ID = "1:513541784733:web:bf478e1b358b0ca2298074"
    const val DATABASE_URL = "https://trading-diary-gm-default-rtdb.firebaseio.com"

    @Volatile
    private var isInitialized = false
    private var appContext: Context? = null

    fun initialize(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            try {
                appContext = context.applicationContext

                val options = FirebaseOptions.Builder()
                    .setApiKey(API_KEY)
                    .setApplicationId(APP_ID)
                    .setProjectId(PROJECT_ID)
                    .setStorageBucket(STORAGE_BUCKET)
                    .setGcmSenderId(MESSAGING_SENDER_ID)
                    .setDatabaseUrl(DATABASE_URL)
                    .build()

                val apps = FirebaseApp.getApps(context)
                if (apps.isEmpty()) {
                    FirebaseApp.initializeApp(context, options)
                    Log.d(TAG, "FirebaseApp initialized with explicit options")
                }

                // Initialize local Room DB
                AppDatabase.getInstance(context)

                // Try configuring Firestore offline cache
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true)
                        .build()
                    firestore.firestoreSettings = settings
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore config notice: ${e.message}")
                }

                // Try configuring Realtime Database persistence
                try {
                    FirebaseDatabase.getInstance().setPersistenceEnabled(true)
                } catch (e: Exception) {
                    Log.w(TAG, "Realtime DB persistence notice: ${e.message}")
                }

                isInitialized = true
                Log.d(TAG, "FirebaseManager successfully initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Firebase initialization error: ${e.message}", e)
                isInitialized = true // proceed gracefully
            }
        }
    }

    val context: Context?
        get() = appContext

    val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth not ready: ${e.message}")
            null
        }

    val database: FirebaseDatabase?
        get() = try {
            FirebaseDatabase.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseDatabase not ready: ${e.message}")
            null
        }

    val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore not ready: ${e.message}")
            null
        }

    val storage: FirebaseStorage?
        get() = try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseStorage not ready: ${e.message}")
            null
        }
}
