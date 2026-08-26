package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TradeDirection
import com.example.data.model.TradeResult
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.BronzeRank
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.CrimsonLossBg
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.EmeraldWinBg
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.IndigoDark
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.SilverRank
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderSubtle
import com.example.ui.theme.SleekCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrophyGold
import com.example.ui.theme.TrophyGoldBg
import java.util.Locale

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = SleekCard,
    borderColor: Color = SleekBorder,
    borderWidth: Dp = 1.dp,
    glowColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = IndigoLight),
            onClick = onClick
        )
    } else Modifier

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            borderColor.copy(alpha = 0.6f),
            borderColor.copy(alpha = 0.15f),
            Color.White.copy(alpha = 0.04f)
        )
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = glowColor ?: Color.Black.copy(alpha = 0.4f),
                spotColor = glowColor ?: Color.Black.copy(alpha = 0.6f)
            )
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderBrush, shape)
            .then(clickableModifier)
    ) {
        content()
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    accentGradient: List<Color> = listOf(IndigoDark, IndigoAccent),
    testTag: String = "glass_button"
) {
    val alpha = if (enabled && !isLoading) 1f else 0.5f

    Box(
        modifier = modifier
            .height(50.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.horizontalGradient(accentGradient.map { it.copy(alpha = alpha) })
            )
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    errorMessage: String? = null,
    testTag: String = "glass_text_field"
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = TextSecondary, fontSize = 13.sp) },
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder, color = TextMuted, fontSize = 14.sp) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            isError = isError,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0x14FFFFFF),
                unfocusedContainerColor = Color(0x0AFFFFFF),
                focusedBorderColor = IndigoLight,
                unfocusedBorderColor = SleekBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = IndigoAccent,
                errorBorderColor = CrimsonLoss
            ),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 54.dp)
                .testTag(testTag)
        )
        if (isError && !errorMessage.isNullOrEmpty()) {
            Text(
                text = errorMessage,
                color = CrimsonLoss,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun DirectionBadge(direction: TradeDirection) {
    val isLong = direction == TradeDirection.LONG
    val bgColor = if (isLong) EmeraldWinBg else CrimsonLossBg
    val tintColor = if (isLong) EmeraldWin else CrimsonLoss
    val text = if (isLong) "LONG" else "SHORT"
    val icon = if (isLong) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, tintColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = tintColor,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = text,
            color = tintColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ResultBadge(result: TradeResult) {
    val (bgColor, textColor, text) = when (result) {
        TradeResult.WIN -> Triple(EmeraldWinBg, EmeraldWin, "WIN")
        TradeResult.LOSS -> Triple(CrimsonLossBg, CrimsonLoss, "LOSS")
        TradeResult.BREAKEVEN -> Triple(Color(0x14FFFFFF), TextSecondary, "BE")
        TradeResult.OPEN -> Triple(TrophyGoldBg, TrophyGold, "OPEN")
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RankBadge(rank: Int, modifier: Modifier = Modifier) {
    val (bgGradient, iconColor, label) = when (rank) {
        1 -> Triple(listOf(TrophyGold, Color(0xFFCA8A04)), Color(0xFF050608), "#1")
        2 -> Triple(listOf(SilverRank, Color(0xFF64748B)), Color(0xFF050608), "#2")
        3 -> Triple(listOf(BronzeRank, Color(0xFF92400E)), Color(0xFFFFFFFF), "#3")
        else -> Triple(listOf(Color(0x1AFFFFFF), Color(0x0DFFFFFF)), TextSecondary, "#$rank")
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(bgGradient))
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
    ) {
        if (rank in 1..3) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Rank $rank",
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                text = label,
                color = iconColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CurrencyPnlText(
    amount: Double,
    fontSize: Int = 16,
    showPlus: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isPositive = amount > 0.0001
    val isNegative = amount < -0.0001
    val color = when {
        isPositive -> EmeraldWin
        isNegative -> CrimsonLoss
        else -> TextSecondary
    }

    val sign = if (isPositive && showPlus) "+" else ""
    val formatted = String.format(Locale.US, "$sign$%,.2f", amount)

    Text(
        text = formatted,
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize.sp,
        modifier = modifier
    )
}

