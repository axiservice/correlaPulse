package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.StockSeries
import com.example.domain.model.SynchronizedPoint
import com.example.domain.model.TimeframeOption
import com.example.ui.theme.CoralBear
import com.example.ui.theme.CoralBearDim
import com.example.ui.theme.EmeraldBull
import com.example.ui.theme.EmeraldBullDim
import com.example.ui.theme.MonoNumericMedium
import com.example.ui.theme.MonoNumericSmall
import com.example.ui.util.FormatUtils
import kotlin.math.roundToInt

@Composable
fun StockChartCard(
    slotNumber: Int,
    series: StockSeries,
    points: List<SynchronizedPoint>,
    isPrimarySlot: Boolean,
    accentColor: Color,
    normalizedPercentMode: Boolean,
    timeframe: TimeframeOption,
    scrubIndex: Int?,
    onScrubChange: (Int?) -> Unit,
    onChangeTickerClick: () -> Unit,
    onRemoveSecondChart: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrubbedPoint = scrubIndex?.let { points.getOrNull(it) }
    val displayPrice = if (scrubbedPoint != null) {
        if (isPrimarySlot) scrubbedPoint.priceA else (scrubbedPoint.priceB ?: series.currentPrice)
    } else {
        series.currentPrice
    }

    val displayPct = if (scrubbedPoint != null) {
        if (isPrimarySlot) scrubbedPoint.normalizedPctA else (scrubbedPoint.normalizedPctB ?: series.changePercent)
    } else {
        series.changePercent
    }

    val isPositive = displayPct >= 0.0
    val deltaColor = if (isPositive) EmeraldBull else CoralBear
    val deltaBg = if (isPositive) EmeraldBullDim else CoralBearDim

    val values = points.mapNotNull { pt ->
        if (normalizedPercentMode) {
            if (isPrimarySlot) pt.normalizedPctA else pt.normalizedPctB
        } else {
            if (isPrimarySlot) pt.priceA else pt.priceB
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "live_dot_$slotNumber")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 11f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stock_chart_card_$slotNumber"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "G$slotNumber",
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onChangeTickerClick)
                            .testTag("ticker_slot_${slotNumber}_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = series.symbol,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Cambia titolo grafico $slotNumber",
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = series.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (scrubbedPoint != null) {
                                FormatUtils.formatTimestamp(
                                    scrubbedPoint.timestampSec,
                                    isDaily = timeframe == TimeframeOption.DAY_1
                                )
                            } else {
                                "${series.exchange} · ${FormatUtils.marketStateLabel(series.marketState)}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (scrubbedPoint != null) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = FormatUtils.formatPrice(displayPrice, series.currency),
                            style = MonoNumericMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(deltaBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = FormatUtils.formatSignedPercent(displayPct),
                                style = MonoNumericSmall,
                                color = deltaColor
                            )
                        }
                    }
                    if (onRemoveSecondChart != null) {
                        IconButton(
                            onClick = onRemoveSecondChart,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("remove_chart_2_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Nascondi secondo grafico",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (values.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "In attesa di punti sincronizzati sufficienti...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val minVal = values.minOrNull() ?: 0.0
                val maxVal = values.maxOrNull() ?: 0.0
                val midVal = (minVal + maxVal) / 2.0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(152.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val gridLineColor = MaterialTheme.colorScheme.outlineVariant
                    val crosshairColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .pointerInput(points.size) {
                                detectHorizontalDragGestures(
                                    onDragStart = { offset ->
                                        val idx = ((offset.x / size.width.coerceAtLeast(1)) * (points.size - 1))
                                            .roundToInt()
                                            .coerceIn(0, points.lastIndex)
                                        onScrubChange(idx)
                                    },
                                    onHorizontalDrag = { change, _ ->
                                        change.consume()
                                        val idx = ((change.position.x / size.width.coerceAtLeast(1)) * (points.size - 1))
                                            .roundToInt()
                                            .coerceIn(0, points.lastIndex)
                                        onScrubChange(idx)
                                    },
                                    onDragEnd = { onScrubChange(null) },
                                    onDragCancel = { onScrubChange(null) }
                                )
                            }
                            .pointerInput(points.size) {
                                detectTapGestures { offset ->
                                    val idx = ((offset.x / size.width.coerceAtLeast(1)) * (points.size - 1))
                                        .roundToInt()
                                        .coerceIn(0, points.lastIndex)
                                    onScrubChange(if (scrubIndex == idx) null else idx)
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val topPad = 10.dp.toPx()
                            val bottomPad = 10.dp.toPx()
                            val plotH = (h - topPad - bottomPad).coerceAtLeast(1f)
                            val range = (maxVal - minVal).takeIf { it > 1e-9 } ?: 1.0

                            // Horizontal grid lines
                            listOf(0f, 0.5f, 1f).forEach { frac ->
                                val y = topPad + plotH * frac
                                drawLine(
                                    color = gridLineColor,
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                                )
                            }

                            val stepX = if (values.size > 1) w / (values.size - 1).toFloat() else w
                            val linePath = Path()
                            val fillPath = Path()

                            values.forEachIndexed { i, v ->
                                val x = i * stepX
                                val normY = ((v - minVal) / range).toFloat()
                                val y = topPad + plotH * (1f - normY)
                                if (i == 0) {
                                    linePath.moveTo(x, y)
                                    fillPath.moveTo(x, h)
                                    fillPath.lineTo(x, y)
                                } else {
                                    linePath.lineTo(x, y)
                                    fillPath.lineTo(x, y)
                                }
                            }
                            fillPath.lineTo((values.size - 1) * stepX, h)
                            fillPath.close()

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.28f),
                                        accentColor.copy(alpha = 0.02f)
                                    ),
                                    startY = topPad,
                                    endY = h
                                )
                            )

                            drawPath(
                                path = linePath,
                                color = accentColor,
                                style = Stroke(
                                    width = 2.2.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )

                            // Synchronized crosshair or live pulsing dot
                            val activeIdx = scrubIndex?.coerceIn(0, values.lastIndex)
                            if (activeIdx != null) {
                                val cx = activeIdx * stepX
                                val cy = topPad + plotH * (1f - ((values[activeIdx] - minVal) / range).toFloat())
                                drawLine(
                                    color = crosshairColor,
                                    start = Offset(cx, 0f),
                                    end = Offset(cx, h),
                                    strokeWidth = 1.2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                                )
                                drawCircle(
                                    color = accentColor.copy(alpha = 0.25f),
                                    radius = 9.dp.toPx(),
                                    center = Offset(cx, cy)
                                )
                                drawCircle(
                                    color = accentColor,
                                    radius = 4.5.dp.toPx(),
                                    center = Offset(cx, cy)
                                )
                            } else {
                                val lx = (values.size - 1) * stepX
                                val ly = topPad + plotH * (1f - ((values.last() - minVal) / range).toFloat())
                                drawCircle(
                                    color = accentColor.copy(alpha = pulseAlpha),
                                    radius = pulseRadius.dp.toPx(),
                                    center = Offset(lx, ly)
                                )
                                drawCircle(
                                    color = accentColor,
                                    radius = 4.dp.toPx(),
                                    center = Offset(lx, ly)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        modifier = Modifier
                            .width(64.dp)
                            .fillMaxSize()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        val formatter: (Double) -> String = { v ->
                            if (normalizedPercentMode) FormatUtils.formatSignedPercent(v)
                            else FormatUtils.formatPrice(v, series.currency)
                        }
                        Text(
                            text = formatter(maxVal),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = formatter(midVal),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                        Text(
                            text = formatter(minVal),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 72.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val isDaily = timeframe == TimeframeOption.DAY_1
                    Text(
                        text = FormatUtils.formatShortAxisTime(points.first().timestampSec, isDaily),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FormatUtils.formatShortAxisTime(points[points.size / 2].timestampSec, isDaily),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FormatUtils.formatShortAxisTime(points.last().timestampSec, isDaily),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
