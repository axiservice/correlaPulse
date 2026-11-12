package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.engine.CorrelationEngine
import com.example.domain.model.CorrelationMode
import com.example.domain.model.CuratedPair
import com.example.domain.model.SynchronizedAnalysis
import com.example.domain.model.TimeframeOption
import com.example.ui.MainUiState
import com.example.ui.MarketFeedState
import com.example.ui.components.CorrelationTrackerCard
import com.example.ui.components.StockChartCard
import com.example.ui.theme.AmberSeriesB
import com.example.ui.theme.CoralBear
import com.example.ui.theme.CyanSeriesA
import com.example.ui.theme.EmeraldBull
import com.example.ui.theme.MonoNumericSmall
import com.example.ui.theme.VioletCorrelation
import com.example.ui.util.FormatUtils

@Composable
fun TerminalScreen(
    uiState: MainUiState,
    curatedPairs: List<CuratedPair>,
    onToggleLivePolling: () -> Unit,
    onRefreshNow: () -> Unit,
    onToggleDarkTheme: () -> Unit,
    onSelectPair: (String, String) -> Unit,
    onTimeframeChange: (TimeframeOption) -> Unit,
    onSwapSymbols: () -> Unit,
    onToggleNormalizedMode: () -> Unit,
    onToggleSecondChart: () -> Unit,
    onScrubChange: (Int?) -> Unit,
    onOpenTickerPicker: (slot: Int) -> Unit,
    onCorrelationModeChange: (CorrelationMode) -> Unit,
    onRollingWindowChange: (Int) -> Unit,
    onOpenCreateAlert: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.prefs
    val activeAnalysis: SynchronizedAnalysis? = when (val feed = uiState.feedState) {
        is MarketFeedState.Ready -> feed.analysis
        is MarketFeedState.Error -> feed.previousAnalysis
        MarketFeedState.Loading -> null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1120.dp)
        ) {
            // Top Terminal Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(CyanSeriesA.copy(alpha = 0.25f), VioletCorrelation.copy(alpha = 0.28f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ShowChart,
                            contentDescription = null,
                            tint = CyanSeriesA,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CorrelaPulse",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        val updatedText = uiState.lastUpdatedMillis?.let {
                            "Agg. ${FormatUtils.formatMillisDateTime(it)}"
                        } ?: "Quotazioni in tempo reale"
                        Text(
                            text = updatedText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Live Polling Pill
                    val liveColor = if (uiState.isLivePolling) EmeraldBull else MaterialTheme.colorScheme.onSurfaceVariant
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = liveColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, liveColor.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onToggleLivePolling)
                            .testTag("live_polling_toggle")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(liveColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isLivePolling) "LIVE · ${uiState.secondsUntilNextPoll}s" else "PAUSA",
                                style = MonoNumericSmall,
                                color = liveColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (uiState.isLivePolling) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (uiState.isLivePolling) "Metti in pausa live" else "Riprendi live",
                                tint = liveColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefreshNow,
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Aggiorna quotazioni ora",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(
                        onClick = onToggleDarkTheme,
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (prefs.darkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = "Cambia tema chiaro/scuro",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            if (uiState.isRefreshing && activeAnalysis != null) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = CyanSeriesA,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // In-App Alert Trigger Banner
            AnimatedVisibility(
                visible = uiState.activeBannerAlert != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                uiState.activeBannerAlert?.let { alert ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("triggered_alert_banner"),
                        shape = RoundedCornerShape(14.dp),
                        color = CoralBear.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, CoralBear)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = CoralBear,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Soglia superata: ${alert.symbolA} / ${alert.symbolB}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "r = ${FormatUtils.formatCorrelation(alert.observedCorrelation)} (${alert.condition.shortLabel} ${FormatUtils.formatThreshold(alert.threshold)})",
                                    style = MonoNumericSmall,
                                    color = CoralBear
                                )
                            }
                            TextButton(onClick = {
                                onDismissBanner()
                                onNavigateToAlerts()
                            }) {
                                Text("Alert", color = CoralBear)
                            }
                            IconButton(onClick = onDismissBanner) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Chiudi avviso",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Transient Error Banner when cached charts are still visible
            if (uiState.feedState is MarketFeedState.Error && activeAnalysis != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.feedState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onRefreshNow) {
                            Text("Riprova")
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Curated Pairs Strip
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(curatedPairs, key = { "${it.symbolA}_${it.symbolB}" }) { pair ->
                            val isSelected = prefs.showSecondChart &&
                                prefs.symbolA.equals(pair.symbolA, ignoreCase = true) &&
                                prefs.symbolB.equals(pair.symbolB, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) CyanSeriesA else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectPair(pair.symbolA, pair.symbolB) }
                                    .testTag("curated_pair_${pair.symbolA}_${pair.symbolB}")
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(
                                        text = pair.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) CyanSeriesA else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = pair.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Timeframe + Sync Controls Row
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TimeframeOption.entries.forEach { tf ->
                                    val selected = prefs.timeframe == tf
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (selected) CyanSeriesA.copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .clickable { onTimeframeChange(tf) }
                                            .padding(horizontal = 10.dp, vertical = 7.dp)
                                            .testTag("timeframe_${tf.shortLabel}")
                                    ) {
                                        Text(
                                            text = tf.shortLabel,
                                            style = MonoNumericSmall,
                                            color = if (selected) CyanSeriesA else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            // Max 2 Charts Indicator Badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (prefs.showSecondChart) "2/2 Grafici Sync" else "1/2 Grafici",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Action Controls: Normalized % vs Price, Swap A<->B, Add/Hide Chart 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (uiState.normalizedPercentMode) AmberSeriesB.copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (uiState.normalizedPercentMode) AmberSeriesB else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(onClick = onToggleNormalizedMode)
                                    .testTag("toggle_normalized_mode")
                            ) {
                                Text(
                                    text = if (uiState.normalizedPercentMode) "Scala: Var %" else "Scala: Prezzo",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (uiState.normalizedPercentMode) AmberSeriesB else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }

                            if (prefs.showSecondChart) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(onClick = onSwapSymbols)
                                        .testTag("swap_symbols_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SwapVert,
                                            contentDescription = "Inverti titoli",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Inverti",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AmberSeriesB.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, AmberSeriesB.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(onClick = onToggleSecondChart)
                                        .testTag("add_chart_2_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AddChart,
                                            contentDescription = "Aggiungi secondo grafico",
                                            tint = AmberSeriesB,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Aggiungi 2° Grafico",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = AmberSeriesB
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Main Content: Loading / Error / Synchronized Charts + Correlation
                if (activeAnalysis == null) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when (val feed = uiState.feedState) {
                                    is MarketFeedState.Error -> {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ErrorOutline,
                                                contentDescription = null,
                                                tint = CoralBear,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "Connessione quotazioni non riuscita",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = feed.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Button(onClick = onRefreshNow) {
                                                Text("Riprova")
                                            }
                                        }
                                    }
                                    else -> {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = CyanSeriesA)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Sincronizzazione quotazioni in corso...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Up to 2 Synchronized Stock Charts (Adaptive side-by-side on wide screens)
                    item {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val isWide = maxWidth >= 820.dp && activeAnalysis.seriesB != null
                            if (isWide) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    StockChartCard(
                                        slotNumber = 1,
                                        series = activeAnalysis.seriesA,
                                        points = activeAnalysis.points,
                                        isPrimarySlot = true,
                                        accentColor = CyanSeriesA,
                                        normalizedPercentMode = uiState.normalizedPercentMode,
                                        timeframe = prefs.timeframe,
                                        scrubIndex = uiState.scrubIndex,
                                        onScrubChange = onScrubChange,
                                        onChangeTickerClick = { onOpenTickerPicker(1) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    activeAnalysis.seriesB?.let { seriesB ->
                                        StockChartCard(
                                            slotNumber = 2,
                                            series = seriesB,
                                            points = activeAnalysis.points,
                                            isPrimarySlot = false,
                                            accentColor = AmberSeriesB,
                                            normalizedPercentMode = uiState.normalizedPercentMode,
                                            timeframe = prefs.timeframe,
                                            scrubIndex = uiState.scrubIndex,
                                            onScrubChange = onScrubChange,
                                            onChangeTickerClick = { onOpenTickerPicker(2) },
                                            onRemoveSecondChart = onToggleSecondChart,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    StockChartCard(
                                        slotNumber = 1,
                                        series = activeAnalysis.seriesA,
                                        points = activeAnalysis.points,
                                        isPrimarySlot = true,
                                        accentColor = CyanSeriesA,
                                        normalizedPercentMode = uiState.normalizedPercentMode,
                                        timeframe = prefs.timeframe,
                                        scrubIndex = uiState.scrubIndex,
                                        onScrubChange = onScrubChange,
                                        onChangeTickerClick = { onOpenTickerPicker(1) }
                                    )
                                    activeAnalysis.seriesB?.let { seriesB ->
                                        StockChartCard(
                                            slotNumber = 2,
                                            series = seriesB,
                                            points = activeAnalysis.points,
                                            isPrimarySlot = false,
                                            accentColor = AmberSeriesB,
                                            normalizedPercentMode = uiState.normalizedPercentMode,
                                            timeframe = prefs.timeframe,
                                            scrubIndex = uiState.scrubIndex,
                                            onScrubChange = onScrubChange,
                                            onChangeTickerClick = { onOpenTickerPicker(2) },
                                            onRemoveSecondChart = onToggleSecondChart
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Correlation Coefficient Tracker Card
                    item {
                        val matchingRules = if (activeAnalysis.seriesB != null) {
                            prefs.alertRules.filter {
                                CorrelationEngine.ruleMatchesPair(
                                    it,
                                    activeAnalysis.seriesA.symbol,
                                    activeAnalysis.seriesB.symbol
                                )
                            }
                        } else {
                            emptyList()
                        }

                        CorrelationTrackerCard(
                            analysis = activeAnalysis,
                            correlationMode = prefs.correlationMode,
                            timeframe = prefs.timeframe,
                            rollingWindow = prefs.rollingWindow,
                            matchingRules = matchingRules,
                            scrubIndex = uiState.scrubIndex,
                            onScrubChange = onScrubChange,
                            onModeChange = onCorrelationModeChange,
                            onRollingWindowChange = onRollingWindowChange,
                            onAddAlertClick = onOpenCreateAlert
                        )
                    }

                    // Tip if scrubbing or single chart
                    item {
                        if (activeAnalysis.seriesB == null) {
                            OutlinedButton(
                                onClick = onToggleSecondChart,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.AddChart, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ripristina 2° Grafico Sincronizzato (${prefs.symbolB})")
                            }
                        } else {
                            Text(
                                text = "Suggerimento: trascina orizzontalmente su qualsiasi grafico per sincronizzare il cursore su tutti i dati.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }
}
