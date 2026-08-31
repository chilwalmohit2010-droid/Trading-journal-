package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppThemeMode
import com.example.data.ThemePreferences
import com.example.data.TradingRepository
import com.example.data.model.LeaderboardEntry
import com.example.data.model.Trade
import com.example.data.model.TradeDirection
import com.example.data.model.TradeResult
import com.example.data.model.TradingStats
import com.example.data.model.UserProfile
import com.example.domain.ScoreCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class TradeFilter {
    ALL,
    WINS,
    LOSSES,
    LONGS,
    SHORTS
}

sealed class AuthUiState {
    object Loading : AuthUiState()
    data class Authenticated(val user: UserProfile) : AuthUiState()
    data class Unauthenticated(val error: String? = null) : AuthUiState()
}

class TradingViewModel(
    private val repository: TradingRepository = TradingRepository()
) : ViewModel() {

    // Initial state is Loading to prevent any login-screen flash while checking persisted auth
    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _allTrades = MutableStateFlow<List<Trade>>(emptyList())
    val allTrades: StateFlow<List<Trade>> = _allTrades.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TradeFilter.ALL)
    val selectedFilter: StateFlow<TradeFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    private val _stats = MutableStateFlow(TradingStats())
    val stats: StateFlow<TradingStats> = _stats.asStateFlow()

    private val _isAddEditTradeOpen = MutableStateFlow(false)
    val isAddEditTradeOpen: StateFlow<Boolean> = _isAddEditTradeOpen.asStateFlow()

    private val _tradeToEdit = MutableStateFlow<Trade?>(null)
    val tradeToEdit: StateFlow<Trade?> = _tradeToEdit.asStateFlow()

    private val _isEditProfileOpen = MutableStateFlow(false)
    val isEditProfileOpen: StateFlow<Boolean> = _isEditProfileOpen.asStateFlow()

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading: StateFlow<Boolean> = _isActionLoading.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val themeMode: StateFlow<AppThemeMode> = ThemePreferences.themeMode

    private var tradesJob: Job? = null
    private var leaderboardJob: Job? = null

    init {
        observeAuth()
        observeGlobalLeaderboard()
    }

    private fun observeAuth() {
        viewModelScope.launch {
            repository.observeAuthState().collectLatest { user ->
                if (user != null) {
                    _currentUser.value = user
                    _authUiState.value = AuthUiState.Authenticated(user)
                    observeTrades(user.uid)
                } else {
                    _currentUser.value = null
                    _authUiState.value = AuthUiState.Unauthenticated()
                    _allTrades.value = emptyList()
                    tradesJob?.cancel()
                }
            }
        }
    }

    private fun observeTrades(uid: String) {
        tradesJob?.cancel()
        tradesJob = viewModelScope.launch {
            repository.observeUserTrades(uid).collectLatest { trades ->
                _allTrades.value = trades
                recomputeStats(trades, _leaderboard.value, uid)
            }
        }
    }

    private fun observeGlobalLeaderboard() {
        leaderboardJob?.cancel()
        leaderboardJob = viewModelScope.launch {
            repository.observeLeaderboard().collectLatest { entries ->
                _leaderboard.value = entries
                val uid = _currentUser.value?.uid ?: ""
                if (uid.isNotEmpty()) {
                    recomputeStats(_allTrades.value, entries, uid)
                }
            }
        }
    }

    private fun recomputeStats(trades: List<Trade>, leaderboardEntries: List<LeaderboardEntry>, currentUid: String) {
        val total = trades.size
        val wins = trades.count { it.result == TradeResult.WIN }
        val losses = trades.count { it.result == TradeResult.LOSS }
        val breakevens = trades.count { it.result == TradeResult.BREAKEVEN }
        val settled = wins + losses
        val winRate = if (settled > 0) (wins.toDouble() / settled.toDouble()) * 100.0 else 0.0
        val totalPnl = trades.sumOf { it.pnl }

        val grossProfit = trades.filter { it.pnl > 0 }.sumOf { it.pnl }
        val grossLoss = abs(trades.filter { it.pnl < 0 }.sumOf { it.pnl })
        val profitFactor = if (grossLoss > 0.000001) grossProfit / grossLoss else if (grossProfit > 0) 99.9 else 0.0

        val validRRs = trades.map { it.riskRewardRatio }.filter { it > 0.0 }
        val avgRR = if (validRRs.isNotEmpty()) validRRs.average() else 0.0

        val currentScore = ScoreCalculator.calculateScore(trades)

        // Determine rank strictly from current leaderboard entries
        val userIndex = if (currentUid.isNotBlank()) {
            leaderboardEntries.indexOfFirst { it.uid == currentUid }
        } else -1
        val rank = if (userIndex >= 0) userIndex + 1 else 1

        _stats.value = TradingStats(
            totalTrades = total,
            wins = wins,
            losses = losses,
            breakevens = breakevens,
            winRate = winRate,
            totalPnl = totalPnl,
            avgRiskReward = avgRR,
            profitFactor = profitFactor,
            currentScore = currentScore,
            rank = rank,
            totalTradersCount = maxOf(1, leaderboardEntries.size)
        )
    }

    fun signUp(username: String, email: String, pass: String) {
        val cleanUsername = username.trim()
        if (cleanUsername.isBlank() || email.isBlank() || pass.isBlank()) {
            _snackbarMessage.value = "Please fill in all fields"
            return
        }
        if (cleanUsername.length < 3 || cleanUsername.length > 24) {
            _snackbarMessage.value = "Username must be between 3 and 24 characters"
            return
        }
        if (!cleanUsername.matches(Regex("^[a-zA-Z0-9_.]+$"))) {
            _snackbarMessage.value = "Username can only contain letters, numbers, dots, and underscores"
            return
        }
        if (pass.length < 6) {
            _snackbarMessage.value = "Password must be at least 6 characters"
            return
        }

        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            _isActionLoading.value = true
            val result = repository.signUp(cleanUsername, email, pass)
            _isActionLoading.value = false
            result.onSuccess { profile ->
                _currentUser.value = profile
                _authUiState.value = AuthUiState.Authenticated(profile)
                _isEditProfileOpen.value = true
                _snackbarMessage.value = "Account created! Complete your profile setup below."
            }.onFailure { err ->
                _authUiState.value = AuthUiState.Unauthenticated(err.localizedMessage ?: "Sign up failed")
                _snackbarMessage.value = err.localizedMessage ?: "Sign up failed. Please try again."
            }
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _snackbarMessage.value = "Please enter both email and password"
            return
        }

        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            _isActionLoading.value = true
            val result = repository.login(email, pass)
            _isActionLoading.value = false
            result.onSuccess { profile ->
                _currentUser.value = profile
                _authUiState.value = AuthUiState.Authenticated(profile)
                _snackbarMessage.value = "Welcome back, @${profile.username}!"
            }.onFailure { err ->
                _authUiState.value = AuthUiState.Unauthenticated(err.localizedMessage ?: "Login failed")
                _snackbarMessage.value = err.localizedMessage ?: "Invalid email or password"
            }
        }
    }

    fun logout() {
        repository.logout()
        _currentUser.value = null
        _authUiState.value = AuthUiState.Unauthenticated()
        _allTrades.value = emptyList()
        _snackbarMessage.value = "Logged out successfully"
    }

    fun setThemeMode(mode: AppThemeMode) {
        ThemePreferences.setThemeMode(mode)
    }

    fun openEditProfile() {
        _isEditProfileOpen.value = true
    }

    fun closeEditProfile() {
        _isEditProfileOpen.value = false
    }

    fun updateProfile(
        username: String,
        displayName: String,
        bio: String,
        photoURL: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = _currentUser.value ?: run {
            onResult(false, "User not authenticated")
            return
        }

        val cleanUsername = username.trim()
        if (cleanUsername.length < 3 || cleanUsername.length > 24) {
            onResult(false, "Username must be between 3 and 24 characters")
            return
        }
        if (!cleanUsername.matches(Regex("^[a-zA-Z0-9_.]+$"))) {
            onResult(false, "Username can only contain letters, numbers, dots, and underscores")
            return
        }

        viewModelScope.launch {
            _isActionLoading.value = true

            // Check if username changed and is available
            if (!cleanUsername.equals(user.username, ignoreCase = true)) {
                val available = repository.isUsernameAvailable(cleanUsername, user.uid)
                if (!available) {
                    _isActionLoading.value = false
                    onResult(false, "Username @$cleanUsername is already taken. Please choose another.")
                    return@launch
                }
            }

            val result = repository.updateProfile(
                uid = user.uid,
                username = cleanUsername,
                displayName = displayName.trim().ifEmpty { cleanUsername },
                bio = bio.trim(),
                photoURL = photoURL
            )
            _isActionLoading.value = false

            result.onSuccess { updatedProfile ->
                _currentUser.value = updatedProfile
                _authUiState.value = AuthUiState.Authenticated(updatedProfile)
                _isEditProfileOpen.value = false
                _snackbarMessage.value = "Profile updated successfully!"
                onResult(true, null)
            }.onFailure { err ->
                _snackbarMessage.value = "Failed to update profile: ${err.localizedMessage}"
                onResult(false, err.localizedMessage)
            }
        }
    }

    fun uploadProfilePhoto(imageBytes: ByteArray, onResult: (String?) -> Unit) {
        val user = _currentUser.value ?: run {
            onResult(null)
            return
        }

        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.uploadProfilePhoto(user.uid, imageBytes)
            _isActionLoading.value = false
            result.onSuccess { url ->
                onResult(url)
            }.onFailure { err ->
                _snackbarMessage.value = "Photo upload failed: ${err.localizedMessage}"
                onResult(null)
            }
        }
    }

    fun setFilter(filter: TradeFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openAddTrade() {
        _tradeToEdit.value = null
        _isAddEditTradeOpen.value = true
    }

    fun openEditTrade(trade: Trade) {
        _tradeToEdit.value = trade
        _isAddEditTradeOpen.value = true
    }

    fun closeAddEditDialog() {
        _isAddEditTradeOpen.value = false
        _tradeToEdit.value = null
    }

    fun saveTrade(trade: Trade) {
        val user = _currentUser.value ?: return
        if (_isActionLoading.value) return

        if (trade.symbol.isBlank()) {
            _snackbarMessage.value = "Please enter an asset or symbol (e.g. BTC/USDT, EUR/USD)"
            return
        }

        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.saveTrade(user.uid, user.username, trade)
            _isActionLoading.value = false
            result.onSuccess {
                _isAddEditTradeOpen.value = false
                _tradeToEdit.value = null
                _snackbarMessage.value = if (trade.id.isEmpty()) "Trade recorded & score updated!" else "Trade updated successfully!"

                // Single reload of user stats from repository
                repository.fetchUserProfile(user.uid).onSuccess { freshProfile ->
                    if (freshProfile != null) {
                        _currentUser.value = freshProfile
                    }
                }
            }.onFailure { err ->
                _snackbarMessage.value = "Failed to save trade: ${err.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun deleteTrade(tradeId: String) {
        val user = _currentUser.value ?: return
        if (_isActionLoading.value) return

        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.deleteTrade(user.uid, user.username, tradeId)
            _isActionLoading.value = false
            result.onSuccess {
                _snackbarMessage.value = "Trade deleted and score recalculated"
                repository.fetchUserProfile(user.uid).onSuccess { freshProfile ->
                    if (freshProfile != null) {
                        _currentUser.value = freshProfile
                    }
                }
            }.onFailure { err ->
                _snackbarMessage.value = "Could not delete trade: ${err.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
