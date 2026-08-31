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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.FirebaseManager
import com.example.ui.AuthUiState
import com.example.ui.TradingViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.theme.LiquidTheme
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseManager.initialize(applicationContext)
        enableEdgeToEdge()

        setContent {
            val viewModel: TradingViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = themeMode) {
                TradingDiaryApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TradingDiaryApp(
    viewModel: TradingViewModel = viewModel()
) {
    val colors = LiquidTheme.colors
    val authState by viewModel.authUiState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allTrades by viewModel.allTrades.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isAddEditOpen by viewModel.isAddEditTradeOpen.collectAsStateWithLifecycle()
    val tradeToEdit by viewModel.tradeToEdit.collectAsStateWithLifecycle()
    val isEditProfileOpen by viewModel.isEditProfileOpen.collectAsStateWithLifecycle()
    val isActionLoading by viewModel.isActionLoading.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    // Key auth transitions strictly on top-level auth stage to prevent duplicate Dashboard instances and ghosting on data updates
    val authStage = when (authState) {
        is AuthUiState.Loading -> "LOADING"
        is AuthUiState.Authenticated -> "AUTHENTICATED"
        is AuthUiState.Unauthenticated -> "UNAUTHENTICATED"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        AnimatedContent(
            targetState = authStage,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith
                        fadeOut(animationSpec = tween(180))
            },
            label = "app_auth_nav_animation"
        ) { stage ->
            when (stage) {
                "AUTHENTICATED" -> {
                    DashboardScreen(
                        currentUser = currentUser,
                        trades = allTrades,
                        stats = stats,
                        leaderboard = leaderboard,
                        selectedFilter = selectedFilter,
                        searchQuery = searchQuery,
                        themeMode = themeMode,
                        isAddEditOpen = isAddEditOpen,
                        tradeToEdit = tradeToEdit,
                        isEditProfileOpen = isEditProfileOpen,
                        isActionLoading = isActionLoading,
                        snackbarMessage = snackbarMessage,
                        onFilterSelect = viewModel::setFilter,
                        onSearchChange = viewModel::setSearchQuery,
                        onOpenAddTrade = viewModel::openAddTrade,
                        onOpenEditTrade = viewModel::openEditTrade,
                        onCloseAddEdit = viewModel::closeAddEditDialog,
                        onSaveTrade = viewModel::saveTrade,
                        onDeleteTrade = viewModel::deleteTrade,
                        onOpenEditProfile = viewModel::openEditProfile,
                        onCloseEditProfile = viewModel::closeEditProfile,
                        onSaveProfile = viewModel::updateProfile,
                        onUploadPhoto = viewModel::uploadProfilePhoto,
                        onThemeChange = viewModel::setThemeMode,
                        onLogout = viewModel::logout,
                        onDismissSnackbar = viewModel::clearSnackbar
                    )
                }
                "LOADING" -> {
                    // Splash Screen to prevent flash of login screen while checking session
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.bg),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(colors.indigoDark, colors.indigoAccent, colors.emeraldWin)
                                        )
                                    )
                                    .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                    contentDescription = "Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "TRADING DIARY GM",
                                color = colors.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            CircularProgressIndicator(
                                color = colors.indigoAccent,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }
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
