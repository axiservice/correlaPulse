package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.engine.CorrelationEngine
import com.example.domain.model.AlertTriggerEvent
import com.example.domain.model.CorrelationAlertRule
import com.example.ui.MainUiState
import com.example.ui.MarketFeedState
import com.example.ui.theme.CoralBear
import com.example.ui.theme.EmeraldBull
import com.example.ui.theme.MonoNumericMedium
import com.example.ui.theme.MonoNumericSmall
import com.example.ui.theme.VioletCorrelation
import com.example.ui.util.FormatUtils

@Composable
fun AlertsScreen(
    uiState: MainUiState,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onAddAlertClick: () -> Unit,
    onEditRule: (CorrelationAlertRule) -> Unit,
    onToggleRule: (String) -> Unit,
    onDeleteRule: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rules = uiState.prefs.alertRules
    val history = uiState.prefs.alertHistory
    val currentAnalysis = (uiState.feedState as? MarketFeedState.Ready)?.analysis
    val currentCorr = currentAnalysis?.latestRollingCorrelation ?: currentAnalysis?.overallCorrelation

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddAlertClick,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Nuovo Alert") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_new_alert")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = "Alert di Correlazione",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Ricevi avvisi automatici quando r supera le soglie impostate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Notification Permission Card if needed
                if (!hasNotificationPermission) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Notifiche Android disabilitate",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Abilita le notifiche per ricevere avvisi push al superamento delle soglie.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = onRequestNotificationPermission) {
                                    Text("Abilita")
                                }
                            }
                        }
                    }
                }

                // Section: Active Rules
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SOGLIE CONFIGURATE (${rules.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = VioletCorrelation
                        )
                        if (currentCorr != null && currentAnalysis?.seriesB != null) {
                            Text(
                                text = "${currentAnalysis.seriesA.symbol}/${currentAnalysis.seriesB.symbol}: r = ${FormatUtils.formatCorrelation(currentCorr)}",
                                style = MonoNumericSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (rules.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsNone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Nessun alert configurato",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Premi \"Nuovo Alert\" per impostare una soglia personalizzata su r.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(rules, key = { it.id }) { rule ->
                        val matchesCurrentPair = currentAnalysis?.seriesB != null &&
                            CorrelationEngine.ruleMatchesPair(
                                rule,
                                currentAnalysis.seriesA.symbol,
                                currentAnalysis.seriesB.symbol
                            )
                        val isCurrentlyBreached = rule.enabled &&
                            matchesCurrentPair &&
                            currentCorr != null &&
                            CorrelationEngine.isConditionMet(rule.condition, rule.threshold, currentCorr)

                        AlertRuleCard(
                            rule = rule,
                            isCurrentlyBreached = isCurrentlyBreached,
                            onToggle = { onToggleRule(rule.id) },
                            onEdit = { onEditRule(rule) },
                            onDelete = { onDeleteRule(rule.id) }
                        )
                    }
                }

                // Section: Triggered History
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CRONOLOGIA SCATTI (${history.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (history.isNotEmpty()) {
                            TextButton(onClick = onClearHistory) {
                                Text("Pulisci")
                            }
                        }
                    }
                }

                if (history.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = "Nessuna soglia ancora superata in questa sessione.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(history, key = { it.id }) { event ->
                        AlertHistoryCard(event = event)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertRuleCard(
    rule: CorrelationAlertRule,
    isCurrentlyBreached: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = when {
        isCurrentlyBreached -> CoralBear
        rule.enabled -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alert_rule_card_${rule.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (isCurrentlyBreached) 1.5.dp else 1.dp, borderColor)
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
                            .background(
                                if (isCurrentlyBreached) CoralBear.copy(alpha = 0.16f)
                                else VioletCorrelation.copy(alpha = 0.16f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (rule.applyToAllPairs) "TUTTE LE COPPIE" else "${rule.symbolA} / ${rule.symbolB}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrentlyBreached) CoralBear else VioletCorrelation,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isCurrentlyBreached) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = "Attivo ora",
                            tint = CoralBear,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Switch(
                    checked = rule.enabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("alert_rule_switch_${rule.id}")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = rule.condition.shortLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = FormatUtils.formatThreshold(rule.threshold),
                            style = MonoNumericMedium,
                            color = if (isCurrentlyBreached) CoralBear else EmeraldBull
                        )
                    }
                    if (rule.note.isNotBlank()) {
                        Text(
                            text = rule.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    rule.lastTriggeredAtMillis?.let { ts ->
                        Text(
                            text = "Ultimo scatto: ${FormatUtils.formatMillisDateTime(ts)} (r=${FormatUtils.formatCorrelation(rule.lastTriggeredValue)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Modifica alert",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_alert_${rule.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Elimina alert",
                            tint = CoralBear.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertHistoryCard(event: AlertTriggerEvent) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${event.symbolA} / ${event.symbolB} · ${event.condition.shortLabel} ${FormatUtils.formatThreshold(event.threshold)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = FormatUtils.formatMillisDateTime(event.triggeredAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CoralBear.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "r = ${FormatUtils.formatCorrelation(event.observedCorrelation)}",
                    style = MonoNumericSmall,
                    color = CoralBear,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
