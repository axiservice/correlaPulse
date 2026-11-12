package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAlert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.example.domain.engine.CorrelationEngine
import com.example.domain.model.AlertCondition
import com.example.domain.model.CorrelationAlertRule
import com.example.domain.model.CorrelationMode
import com.example.domain.model.SynchronizedAnalysis
import com.example.domain.model.TimeframeOption
import com.example.ui.theme.AmberSeriesB
import com.example.ui.theme.CoralBear
import com.example.ui.theme.EmeraldBull
import com.example.ui.theme.MonoNumericLarge
import com.example.ui.theme.MonoNumericSmall
import com.example.ui.theme.VioletCorrelation
import com.example.ui.util.FormatUtils
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CorrelationTrackerCard(
    analysis: SynchronizedAnalysis,
    correlationMode: CorrelationMode,
    timeframe: TimeframeOption,
    rollingWindow: Int,
    matchingRules: List<CorrelationAlertRule>,
    scrubIndex: Int?,
    onScrubChange: (Int?) -> Unit,
    onModeChange: (CorrelationMode) -> Unit,
    onRollingWindowChange: (Int) -> Unit,
    onAddAlertClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrubbedCorr = scrubIndex?.let { analysis.points.getOrNull(it)?.rollingCorrelation }
    val activeCorr = scrubbedCorr ?: analysis.latestRollingCorrelation ?: analysis.overallCorrelation

    val corrColor = when {
        activeCorr == null -> MaterialTheme.colorScheme.onSurfaceVariant
        activeCorr >= 0.55 -> EmeraldBull
        activeCorr >= 0.25 -> VioletCorrelation
        activeCorr > -0.25 -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> CoralBear
    }

    val breachedRules = if (activeCorr != null) {
        matchingRules.filter { it.enabled && CorrelationEngine.isConditionMet(it.condition, it.threshold, activeCorr) }
    } else {
        emptyList()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("correlation_tracker_card"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (breachedRules.isNotEmpty()) 1.5.dp else 1.dp,
            color = if (breachedRules.isNotEmpty()) CoralBear else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header + Mode selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CORRELAZIONE PEARSON (r)",
                            style = MaterialTheme.typography.labelMedium,
                            color = VioletCorrelation,
                            fontWeight = FontWeight.Bold
                        )
                        if (breachedRules.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = "Soglia superata",
                                tint = CoralBear,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = FormatUtils.formatCorrelation(activeCorr),
                            style = MonoNumericLarge,
                            color = corrColor,
                            modifier = Modifier.testTag("correlation_value_text")
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.padding(bottom = 3.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(corrColor.copy(alpha = 0.16f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = CorrelationEngine.describeCorrelationStrength(activeCorr),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = corrColor
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            val overallText = FormatUtils.formatCorrelation(analysis.overallCorrelation)
                            val r2Text = analysis.rSquared?.let { String.format(Locale.US, "%.0f%%", it * 100) } ?: "—"
                            Text(
                                text = "Periodo: $overallText · R² $r2Text · N=${analysis.sampleCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Mode pills (Rendimenti vs Prezzi)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        CorrelationMode.entries.forEach { mode ->
                            val selected = mode == correlationMode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected) VioletCorrelation.copy(alpha = 0.25f)
                                        else Color.Transparent
                                    )
                                    .clickable { onModeChange(mode) }
                                    .padding(horizontal = 9.dp, vertical = 6.dp)
                                    .testTag("corr_mode_${mode.name.lowercase()}")
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) VioletCorrelation else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bipolar Gauge (-1.0 to +1.0) with Alert Threshold Markers
            BipolarCorrelationGauge(
                currentCorrelation = activeCorr,
                rules = matchingRules
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Rolling Correlation Chart (-1.0 to +1.0)
            val rollingPoints = analysis.points
            val hasRolling = rollingPoints.count { it.rollingCorrelation != null } >= 2

            if (!hasRolling) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (analysis.seriesB == null) {
                            "Aggiungi il 2° grafico per calcolare la correlazione sincronizzata"
                        } else {
                            "Punti sovrapposti (${analysis.sampleCount}) insufficienti per la curva rolling (N=${analysis.rollingWindow}). Prova un timeframe più ampio."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(138.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val zeroLineColor = MaterialTheme.colorScheme.outline
                    val crosshairColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .pointerInput(rollingPoints.size) {
                                detectHorizontalDragGestures(
                                    onDragStart = { offset ->
                                        val idx = ((offset.x / size.width.coerceAtLeast(1)) * (rollingPoints.size - 1))
                                            .roundToInt()
                                            .coerceIn(0, rollingPoints.lastIndex)
                                        onScrubChange(idx)
                                    },
                                    onHorizontalDrag = { change, _ ->
                                        change.consume()
                                        val idx = ((change.position.x / size.width.coerceAtLeast(1)) * (rollingPoints.size - 1))
                                            .roundToInt()
                                            .coerceIn(0, rollingPoints.lastIndex)
                                        onScrubChange(idx)
                                    },
                                    onDragEnd = { onScrubChange(null) },
                                    onDragCancel = { onScrubChange(null) }
                                )
                            }
                            .pointerInput(rollingPoints.size) {
                                detectTapGestures { offset ->
                                    val idx = ((offset.x / size.width.coerceAtLeast(1)) * (rollingPoints.size - 1))
                                        .roundToInt()
                                        .coerceIn(0, rollingPoints.lastIndex)
                                    onScrubChange(if (scrubIndex == idx) null else idx)
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val topPad = 8.dp.toPx()
                            val bottomPad = 8.dp.toPx()
                            val plotH = (h - topPad - bottomPad).coerceAtLeast(1f)

                            fun yForCorr(r: Double): Float {
                                val clamped = r.coerceIn(-1.0, 1.0)
                                val norm = ((clamped + 1.0) / 2.0).toFloat() // -1 -> 0, +1 -> 1
                                return topPad + plotH * (1f - norm)
                            }

                            // +1, 0, -1 lines
                            val yZero = yForCorr(0.0)
                            drawLine(
                                color = zeroLineColor,
                                start = Offset(0f, yZero),
                                end = Offset(w, yZero),
                                strokeWidth = 1.2.dp.toPx()
                            )
                            listOf(1.0, -1.0).forEach { bound ->
                                val yb = yForCorr(bound)
                                drawLine(
                                    color = zeroLineColor.copy(alpha = 0.5f),
                                    start = Offset(0f, yb),
                                    end = Offset(w, yb),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                                )
                            }

                            // Enabled alert threshold lines
                            matchingRules.filter { it.enabled }.forEach { rule ->
                                val linesToDraw = when (rule.condition) {
                                    AlertCondition.ABOVE, AlertCondition.BELOW -> listOf(rule.threshold)
                                    AlertCondition.ABS_ABOVE, AlertCondition.ABS_BELOW -> listOf(
                                        abs(rule.threshold),
                                        -abs(rule.threshold)
                                    )
                                }
                                val isBreached = activeCorr != null &&
                                    CorrelationEngine.isConditionMet(rule.condition, rule.threshold, activeCorr)
                                val lineColor = if (isBreached) CoralBear else AmberSeriesB.copy(alpha = 0.78f)
                                linesToDraw.forEach { thr ->
                                    val yt = yForCorr(thr)
                                    drawLine(
                                        color = lineColor,
                                        start = Offset(0f, yt),
                                        end = Offset(w, yt),
                                        strokeWidth = if (isBreached) 1.8.dp.toPx() else 1.2.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                                    )
                                }
                            }

                            val stepX = if (rollingPoints.size > 1) w / (rollingPoints.size - 1).toFloat() else w
                            val path = Path()
                            val fillPath = Path()
                            var started = false
                            var firstX = 0f
                            var lastX = 0f

                            rollingPoints.forEachIndexed { i, pt ->
                                val r = pt.rollingCorrelation ?: return@forEachIndexed
                                val x = i * stepX
                                val y = yForCorr(r)
                                if (!started) {
                                    started = true
                                    firstX = x
                                    path.moveTo(x, y)
                                    fillPath.moveTo(x, yZero)
                                    fillPath.lineTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                    fillPath.lineTo(x, y)
                                }
                                lastX = x
                            }

                            if (started) {
                                fillPath.lineTo(lastX, yZero)
                                fillPath.close()

                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            VioletCorrelation.copy(alpha = 0.28f),
                                            VioletCorrelation.copy(alpha = 0.04f),
                                            CoralBear.copy(alpha = 0.22f)
                                        ),
                                        startY = topPad,
                                        endY = h - bottomPad
                                    )
                                )

                                drawPath(
                                    path = path,
                                    color = VioletCorrelation,
                                    style = Stroke(
                                        width = 2.2.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            // Synchronized scrub crosshair
                            val activeIdx = scrubIndex?.coerceIn(0, rollingPoints.lastIndex)
                            if (activeIdx != null) {
                                val cx = activeIdx * stepX
                                drawLine(
                                    color = crosshairColor,
                                    start = Offset(cx, 0f),
                                    end = Offset(cx, h),
                                    strokeWidth = 1.2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                                )
                                rollingPoints[activeIdx].rollingCorrelation?.let { r ->
                                    val cy = yForCorr(r)
                                    drawCircle(
                                        color = VioletCorrelation.copy(alpha = 0.28f),
                                        radius = 8.dp.toPx(),
                                        center = Offset(cx, cy)
                                    )
                                    drawCircle(
                                        color = VioletCorrelation,
                                        radius = 4.dp.toPx(),
                                        center = Offset(cx, cy)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        modifier = Modifier
                            .width(38.dp)
                            .fillMaxSize()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("+1.0", style = MaterialTheme.typography.labelSmall, color = EmeraldBull)
                        Text("0.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("-1.0", style = MaterialTheme.typography.labelSmall, color = CoralBear)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rolling Window Chips + Quick Add Alert CTA
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Finestra:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(10, 15, 20, 30).forEach { w ->
                        val selected = rollingWindow == w
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) VioletCorrelation.copy(alpha = 0.22f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { onRollingWindowChange(w) }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${w}p",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) VioletCorrelation else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                FilledTonalButton(
                    onClick = onAddAlertClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("quick_add_alert_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddAlert,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Alert Soglia (${matchingRules.count { it.enabled }})",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (matchingRules.any { it.enabled }) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    matchingRules.filter { it.enabled }.forEach { rule ->
                        val isHit = activeCorr != null &&
                            CorrelationEngine.isConditionMet(rule.condition, rule.threshold, activeCorr)
                        val badgeColor = if (isHit) CoralBear else AmberSeriesB
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeColor.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "${rule.condition.shortLabel} ${FormatUtils.formatThreshold(rule.threshold)}",
                                style = MonoNumericSmall,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BipolarCorrelationGauge(
    currentCorrelation: Double?,
    rules: List<CorrelationAlertRule>
) {
    val trackBg = MaterialTheme.colorScheme.surfaceVariant
    val markerColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        ) {
            val w = size.width
            val cy = size.height / 2f
            val barHeight = 6.dp.toPx()

            // Gradient track -1 -> 0 -> +1
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(CoralBear, trackBg, EmeraldBull)
                ),
                topLeft = Offset(0f, cy - barHeight / 2f),
                size = androidx.compose.ui.geometry.Size(w, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2f)
            )

            // Center 0 tick
            drawLine(
                color = markerColor.copy(alpha = 0.35f),
                start = Offset(w / 2f, cy - 7.dp.toPx()),
                end = Offset(w / 2f, cy + 7.dp.toPx()),
                strokeWidth = 1.5.dp.toPx()
            )

            // Threshold markers
            rules.filter { it.enabled }.forEach { rule ->
                val positions = when (rule.condition) {
                    AlertCondition.ABOVE, AlertCondition.BELOW -> listOf(rule.threshold)
                    AlertCondition.ABS_ABOVE, AlertCondition.ABS_BELOW -> listOf(
                        abs(rule.threshold),
                        -abs(rule.threshold)
                    )
                }
                positions.forEach { thr ->
                    val frac = ((thr.coerceIn(-1.0, 1.0) + 1.0) / 2.0).toFloat()
                    val x = frac * w
                    drawCircle(
                        color = AmberSeriesB,
                        radius = 3.5.dp.toPx(),
                        center = Offset(x, cy)
                    )
                }
            }

            // Current r needle
            if (currentCorrelation != null) {
                val frac = ((currentCorrelation.coerceIn(-1.0, 1.0) + 1.0) / 2.0).toFloat()
                val x = frac * w
                drawCircle(
                    color = VioletCorrelation.copy(alpha = 0.35f),
                    radius = 8.dp.toPx(),
                    center = Offset(x, cy)
                )
                drawCircle(
                    color = markerColor,
                    radius = 5.dp.toPx(),
                    center = Offset(x, cy)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("-1.0 Inversa", style = MaterialTheme.typography.labelSmall, color = CoralBear)
            Text("0.0 Neutro", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("+1.0 Sincrona", style = MaterialTheme.typography.labelSmall, color = EmeraldBull)
        }
    }
}
