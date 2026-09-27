package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.ui.theme.LiquidTheme
import com.example.util.TradeImageHelper

/**
 * Chart Screenshots & Image Attachments Selector for:
 * 1. Before Entry (Market Setup / Technical Analysis)
 * 2. After Exit (Outcome / Execution Result)
 *
 * Supports Android zero-permission Photo Picker & direct Camera capture.
 */
@Composable
fun ChartAttachmentSelector(
    beforeScreenshotUri: String,
    afterScreenshotUri: String,
    onBeforeUriChanged: (String) -> Unit,
    onAfterUriChanged: (String) -> Unit,
    onPreviewChart: (isAfter: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LiquidTheme.colors

    // Pickers for Before Entry Chart
    val pickBeforeGallery = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = TradeImageHelper.copyUriToInternalStorage(context, uri, "chart_before")
            if (saved != null) onBeforeUriChanged(saved)
        }
    }

    val captureBeforeCamera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val saved = TradeImageHelper.saveBitmapToInternalStorage(context, bitmap, "cam_before")
            if (saved != null) onBeforeUriChanged(saved)
        }
    }

    // Pickers for After Exit Chart
    val pickAfterGallery = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = TradeImageHelper.copyUriToInternalStorage(context, uri, "chart_after")
            if (saved != null) onAfterUriChanged(saved)
        }
    }

    val captureAfterCamera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val saved = TradeImageHelper.saveBitmapToInternalStorage(context, bitmap, "cam_after")
            if (saved != null) onAfterUriChanged(saved)
        }
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chart_attachment_selector"),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.indigoDark.copy(alpha = 0.4f))
                            .border(1.dp, colors.indigoLight.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = colors.indigoLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CHART SCREENSHOTS",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.5.sp,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Setup & Exit visual attachments",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Text(
                    text = "Lightbox Zoomable",
                    color = colors.indigoLight,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two-column layout: Before Entry vs After Exit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Column 1: Before Entry Setup Chart
                ChartSlotBox(
                    title = "Before Entry",
                    subtext = "Market Setup",
                    imageUri = beforeScreenshotUri,
                    onPickGallery = {
                        pickBeforeGallery.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onCaptureCamera = {
                        captureBeforeCamera.launch(null)
                    },
                    onPreview = { onPreviewChart(false) },
                    onRemove = { onBeforeUriChanged("") },
                    testTag = "slot_before_chart",
                    modifier = Modifier.weight(1f)
                )

                // Column 2: After Exit Outcome Chart
                ChartSlotBox(
                    title = "After Exit",
                    subtext = "Result & Price Action",
                    imageUri = afterScreenshotUri,
                    onPickGallery = {
                        pickAfterGallery.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onCaptureCamera = {
                        captureAfterCamera.launch(null)
                    },
                    onPreview = { onPreviewChart(true) },
                    onRemove = { onAfterUriChanged("") },
                    testTag = "slot_after_chart",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ChartSlotBox(
    title: String,
    subtext: String,
    imageUri: String,
    onPickGallery: () -> Unit,
    onCaptureCamera: () -> Unit,
    onPreview: () -> Unit,
    onRemove: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val colors = LiquidTheme.colors
    val hasImage = imageUri.isNotBlank()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (colors.isDark) Color(0x12FFFFFF) else Color(0x0A0F172A))
            .border(
                1.dp,
                if (hasImage) colors.indigoLight.copy(alpha = 0.5f) else colors.border,
                RoundedCornerShape(16.dp)
            )
            .padding(10.dp)
            .testTag(testTag)
    ) {
        // Slot Header Label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtext,
                    color = colors.textMuted,
                    fontSize = 10.sp
                )
            }

            if (hasImage) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Screenshot",
                        tint = colors.crimsonLoss,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (hasImage) {
            // Thumbnail Preview with Zoom overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .clickable(onClick = onPreview),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = imageUri,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = colors.indigoAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                // Magnify zoom badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xB3000000))
                        .padding(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Tap to enlarge",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        } else {
            // Empty placeholder with Gallery & Camera buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (colors.isDark) Color(0x0DFFFFFF) else Color(0x06000000))
                    .border(
                        1.dp,
                        colors.border.copy(alpha = 0.6f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Gallery Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.indigoDark.copy(alpha = 0.35f))
                                .border(1.dp, colors.indigoLight.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .clickable(onClick = onPickGallery)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Gallery",
                                    tint = colors.indigoLight,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Gallery",
                                    color = colors.indigoLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Camera Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (colors.isDark) Color(0x1AFFFFFF) else Color(0x10000000))
                                .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                                .clickable(onClick = onCaptureCamera)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Camera",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Camera",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Attach screenshot",
                        color = colors.textMuted,
                        fontSize = 9.5.sp
                    )
                }
            }
        }
    }
}
