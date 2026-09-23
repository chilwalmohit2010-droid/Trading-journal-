package com.example.ui.dialogs

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserProfile
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassTextField
import com.example.ui.components.UserAvatar
import com.example.ui.theme.LiquidTheme
import java.io.ByteArrayOutputStream

@Composable
fun EditProfileDialog(
    currentUser: UserProfile,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSaveProfile: (username: String, displayName: String, bio: String, photoURL: String) -> Unit,
    onUploadPhoto: (ByteArray, (String?) -> Unit) -> Unit
) {
    val colors = LiquidTheme.colors
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var username by remember { mutableStateOf(currentUser.username) }
    var displayName by remember { mutableStateOf(currentUser.displayName.ifEmpty { currentUser.username }) }
    var bio by remember { mutableStateOf(currentUser.bio) }
    var photoUrl by remember { mutableStateOf(currentUser.photoURL) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isUploadingPhoto by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                isUploadingPhoto = true
                val bitmap: Bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }

                // Compress bitmap
                val maxDim = 500
                val ratio = minOf(1f, maxDim.toFloat() / maxOf(bitmap.width, bitmap.height))
                val scaledBitmap = if (ratio < 1f) {
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * ratio).toInt(),
                        (bitmap.height * ratio).toInt(),
                        true
                    )
                } else bitmap

                val stream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                val bytes = stream.toByteArray()

                onUploadPhoto(bytes) { newUrl ->
                    isUploadingPhoto = false
                    if (newUrl != null) {
                        photoUrl = newUrl
                    }
                }
            } catch (e: Exception) {
                isUploadingPhoto = false
                errorMessage = "Could not load image: ${e.message}"
            }
        }
    }

    var dialogAppeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        dialogAppeared = true
    }

    val dialogScale by animateFloatAsState(
        targetValue = if (dialogAppeared) 1.0f else 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "edit_profile_spring_scale"
    )
    val dialogAlpha by animateFloatAsState(
        targetValue = if (dialogAppeared) 1.0f else 0.0f,
        animationSpec = tween(180),
        label = "edit_profile_fade"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = dialogScale
                        scaleY = dialogScale
                        alpha = dialogAlpha
                    }
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Consume clicks inside */ }
                    ),
                shape = RoundedCornerShape(24.dp)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Profile",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar Picker
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.padding(8.dp)
                ) {
                    UserAvatar(
                        photoUrl = photoUrl,
                        username = username.ifEmpty { "User" },
                        size = 80.dp
                    )

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.indigoAccent)
                            .border(2.dp, colors.surface, CircleShape)
                            .clickable { galleryLauncher.launch("image/*") }
                            .testTag("picker_change_avatar"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Upload photo",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = if (isUploadingPhoto) "Uploading photo..." else "Tap camera icon to change photo",
                    color = if (isUploadingPhoto) colors.indigoAccent else colors.textMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Form Fields
                GlassTextField(
                    value = username,
                    onValueChange = {
                        username = it.lowercase().replace(" ", "_")
                        errorMessage = null
                    },
                    label = "Username (@)",
                    placeholder = "e.g. alex_trader",
                    leadingIcon = {
                        Text("@", color = colors.textSecondary, fontWeight = FontWeight.Bold)
                    },
                    isError = errorMessage != null,
                    errorMessage = errorMessage,
                    testTag = "input_edit_username"
                )

                Spacer(modifier = Modifier.height(12.dp))

                GlassTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = "Display Name",
                    placeholder = "e.g. Alex Morgan",
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = colors.textSecondary)
                    },
                    testTag = "input_edit_displayname"
                )

                Spacer(modifier = Modifier.height(12.dp))

                GlassTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 150) bio = it },
                    label = "Bio / Trading Strategy (max 150 chars)",
                    placeholder = "e.g. Price Action • ICT • Forex & Crypto",
                    testTag = "input_edit_bio"
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        accentGradient = listOf(Color(0x26FFFFFF), Color(0x14FFFFFF)),
                        testTag = "btn_cancel_edit"
                    )

                    GlassButton(
                        text = "Save Changes",
                        onClick = {
                            val cleanUser = username.trim()
                            if (cleanUser.length < 3) {
                                errorMessage = "Username must be at least 3 characters"
                                return@GlassButton
                            }
                            if (!cleanUser.matches(Regex("^[a-zA-Z0-9_.]+$"))) {
                                errorMessage = "Only letters, numbers, dot and underscore allowed"
                                return@GlassButton
                            }
                            onSaveProfile(cleanUser, displayName, bio, photoUrl)
                        },
                        isLoading = isLoading || isUploadingPhoto,
                        modifier = Modifier.weight(1f),
                        testTag = "btn_save_profile"
                    )
                }
            }
        }
    }
}
}
