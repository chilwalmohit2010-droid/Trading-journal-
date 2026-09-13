package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Trade
import com.example.data.model.TradeResult
import com.example.domain.ScoreCalculator
import com.example.ui.theme.LiquidTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Historical data point representing a user's score at a specific trade event.
 */
data class ScoreTimelinePoint(
    val index: Int,
    val timestamp: Long,
    val score: Long,
    val delta: Long,
    val trade: Trade?,
    val cumulativePnL: Double,
    val cumulativeWins: Int,
    val cumulativeLosses: Int
)

enum class ScoreChartFilter(val label: String) {
    ALL("All Time"),
    LAST_10("Last 10"),
    DAYS_30("30 Days"),
    DAYS_7("7 Days")
}

/**
 * Helper to generate score progression points over time from a trade list.
 */
object ScoreTimelineGenerator {
    fun generateTimeline(trades: List<Trade>): List<ScoreTimelinePoint> {
        if (trades.isEmpty()) return emptyList()

        val sorted = trades.sortedBy { it.timestamp }
        val points = mutableListOf<ScoreTimelinePoint>()
        var previousScore = 0L

        var cumulativeWins = 0
        var cumulativeLosses = 0
        var cumulativePnL = 0.0

        for (i in sorted.indices) {
            val sublist = sorted.subList(0, i + 1)
            val currentTrade = sorted[i]
            val calculatedScore = ScoreCalculator.calculateScore(sublist)
            val delta = if (i == 0) calculatedScore else (calculatedScore - previousScore)
            previousScore = calculatedScore

            if (currentTrade.result == TradeResult.WIN) cumulativeWins++
            if (currentTrade.result == TradeResult.LOSS) cumulativeLosses++
            cumulativePnL += currentTrade.pnl

            points.add(
                ScoreTimelinePoint(
                    index = i + 1,
                    timestamp = currentTrade.timestamp,
                    score = calculatedScore,
                    delta = delta,
                    trade = currentTrade,
                    cumulativePnL = cumulativePnL,
                    cumulativeWins = cumulativeWins,
                    cumulativeLosses = cumulativeLosses
                )
            )
        }

        return points
    }

    fun filterTimeline(points: List<ScoreTimelinePoint>, filter: ScoreChartFilter): List<ScoreTimelinePoint> {
        if (points.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        return when (filter) {
            ScoreChartFilter.ALL -> points
            ScoreChartFilter.LAST_10 -> points.takeLast(10)
            ScoreChartFilter.DAYS_30 -> {
                val cutoff = now - (30L * 24 * 60 * 60 * 1000L)
                val filtered = points.filter { it.timestamp >= cutoff }
                if (filtered.isEmpty()) points.takeLast(5) else filtered
            }
            ScoreChartFilter.DAYS_7 -> {
                val cutoff = now - (7L * 24 * 60 * 60 * 1000L)
                val filtered = points.filter { it.timestamp >= cutoff }
                if (filtered.isEmpty()) points.takeLast(5) else filtered
            }
        }
    }
}

@Composable
fun ScoreProgressionCard(
    trades: List<Trade>,
    currentScore: Long,
    modifier: Modifier = Modifier,
    onScoreGuideClick: () -> Unit = {}
) {
    val colors = LiquidTheme.colors
    var selectedFilter by remember { mutableStateOf(ScoreChartFilter.ALL) }

    val rawTimeline = remember(trades) {
        ScoreTimelineGenerator.generateTimeline(trades)
    }

    val filteredTimeline = remember(rawTimeline, selectedFilter) {
        ScoreTimelineGenerator.filterTimeline(rawTimeline, selectedFilter)
    }

    var activeSelectedIndex by remember { mutableStateOf<Int?>(null) }

    // Clear selection if filtered list changes
    val currentSelectedPoint = remember(activeSelectedIndex, filteredTimeline) {
        activeSelectedIndex?.let { idx ->
            filteredTimeline.getOrNull(idx)
        } ?: filteredTimeline.lastOrNull()
    }

    val maxScoreInView = remember(filteredTimeline) {
        filteredTimeline.maxOfOrNull { it.score } ?: currentScore
    }
    val minScoreInView = remember(filteredTimeline) {
        filteredTimeline.minOfOrNull { it.score } ?: 0L
    }

    val initialScore = filteredTimeline.firstOrNull()?.score ?: 0L
    val latestScore = filteredTimeline.lastOrNull()?.score ?: currentScore
    val totalDelta = latestScore - initialScore

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("score_progression_card"),
        shape = RoundedCornerShape(22.dp),
        borderColor = colors.borderHighlight
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Title & Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.indigoDark.copy(alpha = 0.6f))
                            .border(1.dp, colors.indigoAccent.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Score Progression",
                            tint = colors.indigoAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "SCORE PROGRESSION",
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Historical Score Timeline",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Guide trigger badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                        .clickable { onScoreGuideClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Scoring Guide",
                        color = colors.indigoAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Time Filter Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ScoreChartFilter.values().forEach { filter ->
                    val isSelected = filter == selectedFilter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSelected) colors.indigoAccent else Color.Transparent)
                            .clickable {
                                selectedFilter = filter
                                activeSelectedIndex = null
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.label,
                            color = if (isSelected) Color.White else colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Highlighted point or Summary banner
            if (currentSelectedPoint != null && filteredTimeline.isNotEmpty()) {
                ScoreInspectorHeader(
                    point = currentSelectedPoint,
                    isScrubbing = activeSelectedIndex != null,
                    totalSampleCount = filteredTimeline.size,
                    peakScore = maxScoreInView
                )
            } else {
                // Empty state preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Current Score",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$currentScore PTS",
                            color = colors.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = "0 trades recorded",
                        color = colors.textMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Native Smooth Canvas Line Chart
            if (filteredTimeline.isEmpty()) {
                EmptyScoreChartPlaceholder()
            } else {
                ScoreLineChartCanvas(
                    timeline = filteredTimeline,
                    selectedIndex = activeSelectedIndex,
                    onSelectIndex = { activeSelectedIndex = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom summary footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (totalDelta >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (totalDelta >= 0) colors.emeraldWin else colors.crimsonLoss,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = (if (totalDelta >= 0) "+" else "") + "$totalDelta PTS over period",
                        color = if (totalDelta >= 0) colors.emeraldWin else colors.crimsonLoss,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Touch/drag chart to inspect trades",
                    color = colors.textMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun ScoreInspectorHeader(
    point: ScoreTimelinePoint,
    isScrubbing: Boolean,
    totalSampleCount: Int,
    peakScore: Long
) {
    val colors = LiquidTheme.colors
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()) }
    val formattedDate = remember(point.timestamp) { dateFormat.format(Date(point.timestamp)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isScrubbing) colors.indigoDark.copy(alpha = 0.35f) else colors.surface)
            .border(
                1.dp,
                if (isScrubbing) colors.indigoAccent.copy(alpha = 0.5f) else colors.border,
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${point.score} PTS",
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (point.delta != 0L) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (point.delta > 0) colors.emeraldWinBg else colors.crimsonLossBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = (if (point.delta > 0) "+" else "") + "${point.delta} pts",
                                    color = if (point.delta > 0) colors.emeraldWin else colors.crimsonLoss,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = if (isScrubbing) "After Trade #${point.index} • $formattedDate" else "Latest • $formattedDate",
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                }

                point.trade?.let { trade ->
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DirectionBadge(direction = trade.direction)
                            Spacer(modifier = Modifier.width(4.dp))
                            ResultBadge(result = trade.result)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${trade.symbol} • " + (if (trade.pnl >= 0) "+$" else "-$") + String.format(Locale.US, "%.2f", Math.abs(trade.pnl)),
                            color = if (trade.pnl >= 0) colors.emeraldWin else colors.crimsonLoss,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } ?: run {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Peak: $peakScore PTS",
                            color = colors.trophyGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalSampleCount Total Trades",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreLineChartCanvas(
    timeline: List<ScoreTimelinePoint>,
    selectedIndex: Int?,
    onSelectIndex: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LiquidTheme.colors
    val isDark = colors.isDark

    val primaryLineColor = colors.indigoAccent
    val glowColor = colors.indigoLight
    val areaStartColor = colors.indigoAccent.copy(alpha = if (isDark) 0.38f else 0.25f)
    val areaEndColor = colors.indigoAccent.copy(alpha = 0.0f)
    val gridLineColor = if (isDark) Color(0x1AFFFFFF) else Color(0x140F172A)
    val textLabelColor = if (isDark) Color(0x80FFFFFF) else Color(0x800F172A)

    val dateFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface.copy(alpha = 0.4f))
            .pointerInput(timeline) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        val nearestIdx = ((timeline.size - 1) * ratio).toInt().coerceIn(0, timeline.size - 1)
                        onSelectIndex(nearestIdx)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                        val nearestIdx = ((timeline.size - 1) * ratio).toInt().coerceIn(0, timeline.size - 1)
                        onSelectIndex(nearestIdx)
                    },
                    onDragEnd = { /* Keep selected point or leave as is */ },
                    onDragCancel = { }
                )
            }
            .pointerInput(timeline) {
                detectTapGestures(
                    onTap = { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        val nearestIdx = ((timeline.size - 1) * ratio).toInt().coerceIn(0, timeline.size - 1)
                        onSelectIndex(nearestIdx)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (timeline.isEmpty()) return@Canvas

            val width = size.width
            val height = size.height

            val leftPadding = 8.dp.toPx()
            val rightPadding = 8.dp.toPx()
            val topPadding = 24.dp.toPx()
            val bottomPadding = 24.dp.toPx()

            val chartWidth = width - leftPadding - rightPadding
            val chartHeight = height - topPadding - bottomPadding

            val scores = timeline.map { it.score }
            val rawMin = scores.minOrNull() ?: 0L
            val rawMax = scores.maxOrNull() ?: 1000L

            // Ensure vertical range has headroom
            val scorePadding = maxOf(50L, ((rawMax - rawMin) * 0.15f).toLong())
            val minScore = maxOf(0L, rawMin - scorePadding)
            val maxScore = maxOf(100L, rawMax + scorePadding)
            val scoreSpan = maxOf(1L, maxScore - minScore)

            // Draw horizontal gridlines & guide text
            val gridSteps = 3
            for (i in 0..gridSteps) {
                val stepRatio = i.toFloat() / gridSteps.toFloat()
                val y = topPadding + chartHeight * (1f - stepRatio)
                val gridScore = minScore + (scoreSpan * stepRatio).toLong()

                // Dashed horizontal line
                drawLine(
                    color = gridLineColor,
                    start = Offset(leftPadding, y),
                    end = Offset(width - rightPadding, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )

                // Grid score label
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = textLabelColor.hashCode()
                        textSize = 9.sp.toPx()
                        isAntiAlias = true
                    }
                    drawText("$gridScore", leftPadding + 4.dp.toPx(), y - 4.dp.toPx(), paint)
                }
            }

            // Calculate coordinates for each point
            val pointOffsets = timeline.mapIndexed { index, point ->
                val x = if (timeline.size == 1) {
                    leftPadding + chartWidth / 2f
                } else {
                    leftPadding + (index.toFloat() / (timeline.size - 1).toFloat()) * chartWidth
                }
                val yRatio = ((point.score - minScore).toFloat() / scoreSpan.toFloat()).coerceIn(0f, 1f)
                val y = topPadding + chartHeight * (1f - yRatio)
                Offset(x, y)
            }

            if (pointOffsets.size == 1) {
                // Single point representation
                val single = pointOffsets.first()
                drawCircle(
                    color = primaryLineColor,
                    radius = 6.dp.toPx(),
                    center = single
                )
            } else {
                // Construct smooth cubic bezier curve
                val path = Path()
                val fillPath = Path()

                path.moveTo(pointOffsets[0].x, pointOffsets[0].y)
                fillPath.moveTo(pointOffsets[0].x, topPadding + chartHeight)
                fillPath.lineTo(pointOffsets[0].x, pointOffsets[0].y)

                for (i in 0 until pointOffsets.size - 1) {
                    val p0 = pointOffsets[maxOf(0, i - 1)]
                    val p1 = pointOffsets[i]
                    val p2 = pointOffsets[i + 1]
                    val p3 = pointOffsets[minOf(pointOffsets.size - 1, i + 2)]

                    val controlPoint1 = Offset(
                        p1.x + (p2.x - p0.x) * 0.18f,
                        p1.y + (p2.y - p0.y) * 0.18f
                    )
                    val controlPoint2 = Offset(
                        p2.x - (p3.x - p1.x) * 0.18f,
                        p2.y - (p3.y - p1.y) * 0.18f
                    )

                    path.cubicTo(
                        controlPoint1.x, controlPoint1.y,
                        controlPoint2.x, controlPoint2.y,
                        p2.x, p2.y
                    )
                    fillPath.cubicTo(
                        controlPoint1.x, controlPoint1.y,
                        controlPoint2.x, controlPoint2.y,
                        p2.x, p2.y
                    )
                }

                // Close fill area
                fillPath.lineTo(pointOffsets.last().x, topPadding + chartHeight)
                fillPath.close()

                // Draw Gradient Fill Area
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(areaStartColor, areaEndColor),
                        startY = topPadding,
                        endY = topPadding + chartHeight
                    )
                )

                // Draw Main Glow Stroke Line
                drawPath(
                    path = path,
                    color = primaryLineColor,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Draw X-Axis Date Labels (First, Middle, Last)
            if (timeline.size >= 2) {
                val labelIndices = if (timeline.size > 2) listOf(0, timeline.size / 2, timeline.size - 1) else listOf(0, 1)
                for (idx in labelIndices) {
                    val pt = pointOffsets[idx]
                    val dateStr = dateFormat.format(Date(timeline[idx].timestamp))
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = textLabelColor.hashCode()
                            textSize = 9.sp.toPx()
                            isAntiAlias = true
                            textAlign = if (idx == 0) android.graphics.Paint.Align.LEFT
                                        else if (idx == timeline.size - 1) android.graphics.Paint.Align.RIGHT
                                        else android.graphics.Paint.Align.CENTER
                        }
                        drawText(dateStr, pt.x, height - 4.dp.toPx(), paint)
                    }
                }
            }

            // Draw Active Selected Point Guide & Indicator Node
            val effectiveIndex = selectedIndex ?: (timeline.size - 1)
            if (effectiveIndex in pointOffsets.indices) {
                val activePt = pointOffsets[effectiveIndex]

                // Vertical dashed cursor
                drawLine(
                    color = primaryLineColor.copy(alpha = 0.7f),
                    start = Offset(activePt.x, topPadding),
                    end = Offset(activePt.x, topPadding + chartHeight),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // Outer ambient glow ring
                drawCircle(
                    color = glowColor.copy(alpha = 0.35f),
                    radius = 12.dp.toPx(),
                    center = activePt
                )

                // Mid ring
                drawCircle(
                    color = primaryLineColor,
                    radius = 6.dp.toPx(),
                    center = activePt
                )

                // Inner core
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = activePt
                )
            }
        }
    }
}

@Composable
private fun EmptyScoreChartPlaceholder() {
    val colors = LiquidTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface.copy(alpha = 0.4f))
            .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Decorative faint curve in background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(0f, size.height * 0.7f)
                cubicTo(
                    size.width * 0.3f, size.height * 0.65f,
                    size.width * 0.6f, size.height * 0.4f,
                    size.width, size.height * 0.35f
                )
            }
            drawPath(
                path = path,
                color = colors.indigoAccent.copy(alpha = 0.15f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoGraph,
                contentDescription = null,
                tint = colors.indigoAccent.copy(alpha = 0.6f),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "No Historical Trades Yet",
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Record trades in the journal to plot your score progression",
                color = colors.textMuted,
                fontSize = 11.sp
            )
        }
    }
}
