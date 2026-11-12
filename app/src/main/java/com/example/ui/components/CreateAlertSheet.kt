package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.engine.CorrelationEngine
import com.example.domain.model.AlertCondition
import com.example.domain.model.CorrelationAlertRule
import com.example.ui.theme.CoralBear
import com.example.ui.theme.EmeraldBull
import com.example.ui.theme.MonoNumericLarge
import com.example.ui.theme.VioletCorrelation
import com.example.ui.util.FormatUtils
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateAlertSheet(
    symbolA: String,
    symbolB: String,
    currentCorrelation: Double?,
    editingRule: CorrelationAlertRule? = null,
    onRequestNotificationPermission: () -> Unit,
    onSave: (
        existingId: String?,
        symbolA: String,
        symbolB: String,
        applyToAllPairs: Boolean,
        condition: AlertCondition,
        threshold: Double,
        systemNotification: Boolean,
        note: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var applyToAll by remember(editingRule) { mutableStateOf(editingRule?.applyToAllPairs ?: false) }
    var condition by remember(editingRule) { mutableStateOf(editingRule?.condition ?: AlertCondition.ABOVE) }
    var threshold by remember(editingRule) { mutableDoubleStateOf(editingRule?.threshold ?: 0.80) }
    var systemNotification by remember(editingRule) { mutableStateOf(editingRule?.systemNotification ?: true) }
    var note by remember(editingRule) { mutableStateOf(editingRule?.note ?: "") }

    val isAbsCondition = condition == AlertCondition.ABS_ABOVE || condition == AlertCondition.ABS_BELOW
    val sliderMin = if (isAbsCondition) 0.05f else -0.95f
    val sliderMax = 0.95f

    if (isAbsCondition && threshold < 0.05) {
        threshold = 0.25
    }

    val wouldTriggerNow = currentCorrelation != null &&
        CorrelationEngine.isConditionMet(condition, threshold, currentCorrelation)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingRule != null) "Modifica Alert Correlazione" else "Nuovo Alert Correlazione",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Chiudi")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pair Scope
            Text(
                text = "Coppia monitorata",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val targetPairLabel = "${editingRule?.symbolA ?: symbolA} / ${editingRule?.symbolB ?: symbolB}"
                listOf(false to targetPairLabel, true to "Tutte le coppie").forEach { (allPairs, label) ->
                    val selected = applyToAll == allPairs
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { applyToAll = allPairs }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Condition Picker
            Text(
                text = "Condizione di scatto",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlertCondition.entries.forEach { cond ->
                    val selected = condition == cond
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (selected) VioletCorrelation else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                condition = cond
                                if ((cond == AlertCondition.ABS_ABOVE || cond == AlertCondition.ABS_BELOW) && threshold < 0.05) {
                                    threshold = 0.30
                                }
                            }
                            .testTag("alert_condition_${cond.name.lowercase()}")
                    ) {
                        Text(
                            text = cond.shortLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) VioletCorrelation else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Threshold Slider + Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Soglia coefficiente r",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = condition.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (isAbsCondition) FormatUtils.formatThreshold(threshold).removePrefix("+")
                    else FormatUtils.formatThreshold(threshold),
                    style = MonoNumericLarge,
                    color = VioletCorrelation,
                    modifier = Modifier.testTag("alert_threshold_value")
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        threshold = ((threshold - 0.05).coerceIn(sliderMin.toDouble(), sliderMax.toDouble()) * 100).roundToInt() / 100.0
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Diminuisci soglia")
                }

                Slider(
                    value = threshold.toFloat().coerceIn(sliderMin, sliderMax),
                    onValueChange = { raw ->
                        threshold = (raw * 20f).roundToInt() / 20.0
                    },
                    valueRange = sliderMin..sliderMax,
                    colors = SliderDefaults.colors(
                        thumbColor = VioletCorrelation,
                        activeTrackColor = VioletCorrelation
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("alert_threshold_slider")
                )

                IconButton(
                    onClick = {
                        threshold = ((threshold + 0.05).coerceIn(sliderMin.toDouble(), sliderMax.toDouble()) * 100).roundToInt() / 100.0
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Aumenta soglia")
                }
            }

            if (currentCorrelation != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (wouldTriggerNow) CoralBear.copy(alpha = 0.14f)
                            else EmeraldBull.copy(alpha = 0.12f)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (wouldTriggerNow) {
                            "Attenzione: con r attuale (${FormatUtils.formatCorrelation(currentCorrelation)}) la soglia è già superata."
                        } else {
                            "Stato attuale: r = ${FormatUtils.formatCorrelation(currentCorrelation)} (sotto controllo)."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (wouldTriggerNow) CoralBear else EmeraldBull
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                singleLine = true,
                label = { Text("Nota opzionale (es. Rischio divergenza)") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notifica di sistema Android",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Invia una notifica push locale allo scatto",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = systemNotification,
                    onCheckedChange = { checked ->
                        systemNotification = checked
                        if (checked) onRequestNotificationPermission()
                    },
                    modifier = Modifier.testTag("alert_system_notif_switch")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (systemNotification) onRequestNotificationPermission()
                    onSave(
                        editingRule?.id,
                        editingRule?.symbolA ?: symbolA,
                        editingRule?.symbolB ?: symbolB,
                        applyToAll,
                        condition,
                        threshold,
                        systemNotification,
                        note
                    )
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_alert_button")
            ) {
                Text(
                    text = if (editingRule != null) "Salva Modifiche" else "Attiva Alert",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
