package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.remote.NetworkModule
import com.example.domain.model.AlertCondition
import com.example.domain.model.AlertTriggerEvent
import com.example.domain.model.CorrelationAlertRule
import com.example.domain.model.CorrelationMode
import com.example.domain.model.TimeframeOption
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "correlapulse_prefs")

data class UserPreferencesState(
    val symbolA: String = "NVDA",
    val symbolB: String = "AMD",
    val showSecondChart: Boolean = true,
    val timeframe: TimeframeOption = TimeframeOption.MIN_5,
    val correlationMode: CorrelationMode = CorrelationMode.RETURNS,
    val rollingWindow: Int = 20,
    val darkTheme: Boolean = true,
    val alertRules: List<CorrelationAlertRule> = defaultStarterRules(),
    val alertHistory: List<AlertTriggerEvent> = emptyList()
)

private fun defaultStarterRules(): List<CorrelationAlertRule> = listOf(
    CorrelationAlertRule(
        id = "starter_high_corr",
        symbolA = "NVDA",
        symbolB = "AMD",
        applyToAllPairs = false,
        condition = AlertCondition.ABOVE,
        threshold = 0.80,
        enabled = true,
        systemNotification = true,
        note = "Forte sincronia semiconduttori"
    ),
    CorrelationAlertRule(
        id = "starter_decorr",
        symbolA = "NVDA",
        symbolB = "AMD",
        applyToAllPairs = true,
        condition = AlertCondition.ABS_BELOW,
        threshold = 0.25,
        enabled = true,
        systemNotification = true,
        note = "Segnale di decorrelazione"
    )
)

class AlertPreferencesRepository(private val context: Context) {

    private object Keys {
        val SYMBOL_A = stringPreferencesKey("symbol_a")
        val SYMBOL_B = stringPreferencesKey("symbol_b")
        val SHOW_SECOND_CHART = booleanPreferencesKey("show_second_chart")
        val TIMEFRAME = stringPreferencesKey("timeframe")
        val CORRELATION_MODE = stringPreferencesKey("correlation_mode")
        val ROLLING_WINDOW = intPreferencesKey("rolling_window")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val ALERT_RULES_JSON = stringPreferencesKey("alert_rules_json")
        val ALERT_HISTORY_JSON = stringPreferencesKey("alert_history_json")
    }

    private val rulesAdapter = NetworkModule.moshi.adapter<List<CorrelationAlertRule>>(
        Types.newParameterizedType(List::class.java, CorrelationAlertRule::class.java)
    )

    private val historyAdapter = NetworkModule.moshi.adapter<List<AlertTriggerEvent>>(
        Types.newParameterizedType(List::class.java, AlertTriggerEvent::class.java)
    )

    val preferencesFlow: Flow<UserPreferencesState> = context.dataStore.data.map { prefs ->
        val timeframe = prefs[Keys.TIMEFRAME]
            ?.let { runCatching { TimeframeOption.valueOf(it) }.getOrNull() }
            ?: TimeframeOption.MIN_5

        val mode = prefs[Keys.CORRELATION_MODE]
            ?.let { runCatching { CorrelationMode.valueOf(it) }.getOrNull() }
            ?: CorrelationMode.RETURNS

        val rules = prefs[Keys.ALERT_RULES_JSON]?.let { json ->
            runCatching { rulesAdapter.fromJson(json) }.getOrNull()
        } ?: defaultStarterRules()

        val history = prefs[Keys.ALERT_HISTORY_JSON]?.let { json ->
            runCatching { historyAdapter.fromJson(json) }.getOrNull()
        } ?: emptyList()

        UserPreferencesState(
            symbolA = prefs[Keys.SYMBOL_A] ?: "NVDA",
            symbolB = prefs[Keys.SYMBOL_B] ?: "AMD",
            showSecondChart = prefs[Keys.SHOW_SECOND_CHART] ?: true,
            timeframe = timeframe,
            correlationMode = mode,
            rollingWindow = prefs[Keys.ROLLING_WINDOW] ?: timeframe.defaultRollingWindow,
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            alertRules = rules,
            alertHistory = history
        )
    }

    suspend fun updateSymbols(symbolA: String, symbolB: String, showSecondChart: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SYMBOL_A] = symbolA.uppercase().trim()
            prefs[Keys.SYMBOL_B] = symbolB.uppercase().trim()
            prefs[Keys.SHOW_SECOND_CHART] = showSecondChart
        }
    }

    suspend fun setShowSecondChart(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_SECOND_CHART] = show
        }
    }

    suspend fun updateTimeframe(timeframe: TimeframeOption) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TIMEFRAME] = timeframe.name
            prefs[Keys.ROLLING_WINDOW] = timeframe.defaultRollingWindow
        }
    }

    suspend fun updateCorrelationMode(mode: CorrelationMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CORRELATION_MODE] = mode.name
        }
    }

    suspend fun updateRollingWindow(window: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ROLLING_WINDOW] = window.coerceIn(5, 60)
        }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = dark
        }
    }

    suspend fun saveAlertRules(rules: List<CorrelationAlertRule>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ALERT_RULES_JSON] = rulesAdapter.toJson(rules)
        }
    }

    suspend fun appendAlertHistory(newEvents: List<AlertTriggerEvent>) {
        if (newEvents.isEmpty()) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.ALERT_HISTORY_JSON]?.let {
                runCatching { historyAdapter.fromJson(it) }.getOrNull()
            } ?: emptyList()
            val merged = (newEvents + current).take(50)
            prefs[Keys.ALERT_HISTORY_JSON] = historyAdapter.toJson(merged)
        }
    }

    suspend fun clearAlertHistory() {
        context.dataStore.edit { prefs ->
            prefs[Keys.ALERT_HISTORY_JSON] = "[]"
        }
    }

    fun newRuleId(): String = UUID.randomUUID().toString()
}
