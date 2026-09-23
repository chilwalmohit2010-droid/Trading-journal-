package com.example.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Trade
import com.example.ui.components.GlassmorphicTradeEntryForm
import com.example.ui.theme.LiquidTheme

/**
 * Trade Entry Dialog wrapper.
 *
 * NOTE: As required, the screen behind the entry form is NOT visible,
 * meaning it is completely opaque and non-transparent.
 */
@Composable
fun AddEditTradeDialog(
    trade: Trade?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (Trade) -> Unit
) {
    val colors = LiquidTheme.colors

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        BackHandler(onBack = onDismiss)

        // Non-transparent, 100% opaque solid background ensuring the underlying screen is not visible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bg)
        ) {
            GlassmorphicTradeEntryForm(
                trade = trade,
                isLoading = isLoading,
                onDismiss = onDismiss,
                onSave = onSave,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
