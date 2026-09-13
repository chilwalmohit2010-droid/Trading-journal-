package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassTextField
import com.example.ui.theme.LiquidTheme

@Composable
fun AuthScreen(
    isLoading: Boolean,
    onSignUp: (username: String, email: String, pass: String) -> Unit,
    onLogin: (email: String, pass: String) -> Unit
) {
    val colors = LiquidTheme.colors
    var isSignUpMode by remember { mutableStateOf(false) }

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colors.indigoAccent.copy(alpha = if (colors.isDark) 0.15f else 0.08f),
                            Color.Transparent
                        ),
                        radius = 800f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // App Emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(colors.indigoDark, colors.indigoAccent, colors.emeraldWin)
                        )
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = "Trading Diary Logo",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "TRADING DIARY",
                color = colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )

            Text(
                text = "Track your edge • Score discipline • Compete",
                color = colors.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Auth Glass Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_card"),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Segmented Mode Switcher (Log In / Sign Up)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (colors.isDark) Color(0x14FFFFFF) else Color(0x140F172A))
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            .padding(3.dp)
                    ) {
                        val loginBrush = if (!isSignUpMode) Brush.horizontalGradient(listOf(colors.indigoDark, colors.indigoAccent)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        val signupBrush = if (isSignUpMode) Brush.horizontalGradient(listOf(colors.indigoDark, colors.indigoAccent)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(9.dp))
                                .background(loginBrush)
                                .clickable { isSignUpMode = false }
                                .testTag("tab_login")
                        ) {
                            Text(
                                text = "LOG IN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isSignUpMode) Color.White else colors.textSecondary
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(9.dp))
                                .background(signupBrush)
                                .clickable { isSignUpMode = true }
                                .testTag("tab_signup")
                        ) {
                            Text(
                                text = "SIGN UP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSignUpMode) Color.White else colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Sign Up: Username input
                    AnimatedVisibility(
                        visible = isSignUpMode,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column {
                            GlassTextField(
                                value = username,
                                onValueChange = { username = it.lowercase().replace(" ", "_") },
                                label = "Username / Trader Tag",
                                placeholder = "e.g. satoshi_trader",
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = colors.textSecondary
                                    )
                                },
                                testTag = "input_username"
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    // Email input
                    GlassTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address",
                        placeholder = "trader@example.com",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = colors.textSecondary
                            )
                        },
                        testTag = "input_email"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password input
                    GlassTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        placeholder = "••••••••",
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = colors.textSecondary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = colors.textSecondary
                                )
                            }
                        },
                        testTag = "input_password"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Submit Action
                    GlassButton(
                        text = if (isSignUpMode) "CREATE TRADER ACCOUNT" else "LOG IN TO DASHBOARD",
                        onClick = {
                            if (isSignUpMode) {
                                onSignUp(username, email, password)
                            } else {
                                onLogin(email, password)
                            }
                        },
                        isLoading = isLoading,
                        accentGradient = listOf(colors.indigoDark, colors.indigoAccent),
                        testTag = "btn_submit_auth"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isSignUpMode) "Already have an account? Log In" else "New trader? Create account & claim 1,000 Pts",
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { isSignUpMode = !isSignUpMode }
                            .padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
