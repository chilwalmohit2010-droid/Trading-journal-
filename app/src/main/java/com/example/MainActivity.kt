package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.FirebaseManager
import com.example.ui.AuthUiState
import com.example.ui.TradingViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SleekBg

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseManager.initialize(applicationContext)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                TradingDiaryApp()
            }
        }
    }
}

@Composable
fun TradingDiaryApp(
    viewModel: TradingViewModel = viewModel()
) {
    val authState by viewModel.authUiState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allTrades by viewModel.allTrades.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isAddEditOpen by viewModel.isAddEditTradeOpen.collectAsStateWithLifecycle()
    val tradeToEdit by viewModel.tradeToEdit.collectAsStateWithLifecycle()
    val isActionLoading by viewModel.isActionLoading.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
    ) {
        AnimatedContent(
            targetState = authState,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(250))
            },
            label = "app_auth_nav_animation"
        ) { state ->
            when (state) {
                is AuthUiState.Authenticated -> {
                    DashboardScreen(
                        currentUser = state.user,
                        trades = allTrades,
                        stats = stats,
                        leaderboard = leaderboard,
                        selectedFilter = selectedFilter,
                        searchQuery = searchQuery,
                        isAddEditOpen = isAddEditOpen,
                        tradeToEdit = tradeToEdit,
                        isActionLoading = isActionLoading,
                        snackbarMessage = snackbarMessage,
                        onFilterSelect = viewModel::setFilter,
                        onSearchChange = viewModel::setSearchQuery,
                        onOpenAddTrade = viewModel::openAddTrade,
                        onOpenEditTrade = viewModel::openEditTrade,
                        onCloseAddEdit = viewModel::closeAddEditDialog,
                        onSaveTrade = viewModel::saveTrade,
                        onDeleteTrade = viewModel::deleteTrade,
                        onLogout = viewModel::logout,
                        onDismissSnackbar = viewModel::clearSnackbar
                    )
                }
                is AuthUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = IndigoAccent,
                            strokeWidth = 3.dp
                        )
                    }
                }
                else -> {
                    AuthScreen(
                        isLoading = isActionLoading,
                        onSignUp = viewModel::signUp,
                        onLogin = viewModel::login
                    )
                }
            }
        }
    }
}

