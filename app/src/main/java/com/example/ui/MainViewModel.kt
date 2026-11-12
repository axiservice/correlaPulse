package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AlertPreferencesRepository
import com.example.data.local.UserPreferencesState
import com.example.data.remote.NetworkModule
import com.example.data.repository.MarketRepository
import com.example.domain.engine.CorrelationEngine
import com.example.domain.model.AlertCondition
import com.example.domain.model.AlertTriggerEvent
import com.example.domain.model.CorrelationAlertRule
import com.example.domain.model.CorrelationMode
import com.example.domain.model.CuratedPair
import com.example.domain.model.SynchronizedAnalysis
import com.example.domain.model.TickerSearchResult
import com.example.domain.model.TimeframeOption
import com.example.notifications.AlertNotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface MarketFeedState {
    data object Loading : MarketFeedState
    data class Ready(val analysis: SynchronizedAnalysis) : MarketFeedState
    data class Error(val message: String, val previousAnalysis: SynchronizedAnalysis? = null) : MarketFeedState
}

data class MainUiState(
    val prefs: UserPreferencesState = UserPreferencesState(),
    val feedState: MarketFeedState = MarketFeedState.Loading,
    val isRefreshing: Boolean = false,
    val isLivePolling: Boolean = true,
    val secondsUntilNextPoll: Int = 5,
    val lastUpdatedMillis: Long? = null,
    val scrubIndex: Int? = null,
    val normalizedPercentMode: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<TickerSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val activeBannerAlert: AlertTriggerEvent? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val marketRepository = MarketRepository(NetworkModule.yahooFinanceApi)
    private val prefsRepository = AlertPreferencesRepository(application.applicationContext)
    private val notificationHelper = AlertNotificationHelper(application.applicationContext)

    private val _uiState = MutableStateFlow(
        MainUiState(searchResults = marketRepository.quickTickers)
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val curatedPairs: List<CuratedPair> = marketRepository.curatedPairs

    private var pollingJob: Job? = null
    private var fetchJob: Job? = null
    private var searchJob: Job? = null

    init {
        observePreferences()
        startPollingLoop()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            prefsRepository.preferencesFlow.collect { prefs ->
                _uiState.update { it.copy(prefs = prefs) }
            }
        }

        viewModelScope.launch {
            prefsRepository.preferencesFlow
                .map {
                    listOf(
                        it.symbolA,
                        it.symbolB,
                        it.showSecondChart.toString(),
                        it.timeframe.name,
                        it.correlationMode.name,
                        it.rollingWindow.toString()
                    )
                }
                .distinctUntilChanged()
                .collect {
                    val currentAnalysis = (_uiState.value.feedState as? MarketFeedState.Ready)?.analysis
                    fetchMarketData(showLoadingSpinner = currentAnalysis == null)
                }
        }
    }

    private fun startPollingLoop() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.isLivePolling) {
                    for (sec in 5 downTo 1) {
                        if (!_uiState.value.isLivePolling) break
                        _uiState.update { it.copy(secondsUntilNextPoll = sec) }
                        delay(1_000L)
                    }
                    if (_uiState.value.isLivePolling) {
                        fetchMarketData(showLoadingSpinner = false)
                    }
                } else {
                    delay(500L)
                }
            }
        }
    }

    fun refreshNow() {
        fetchMarketData(showLoadingSpinner = (_uiState.value.feedState as? MarketFeedState.Ready)?.analysis == null)
    }

    private fun fetchMarketData(showLoadingSpinner: Boolean) {
        val prefs = _uiState.value.prefs
        val symbolA = prefs.symbolA
        val symbolB = if (prefs.showSecondChart) prefs.symbolB else null

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            if (showLoadingSpinner) {
                _uiState.update { it.copy(feedState = MarketFeedState.Loading, isRefreshing = true) }
            } else {
                _uiState.update { it.copy(isRefreshing = true) }
            }

            val result = marketRepository.fetchSynchronizedPair(
                symbolA = symbolA,
                symbolB = symbolB,
                timeframe = prefs.timeframe
            )

            result.fold(
                onSuccess = { (seriesA, seriesB) ->
                    val analysis = CorrelationEngine.analyze(
                        seriesA = seriesA,
                        seriesB = seriesB,
                        timeframe = prefs.timeframe,
                        mode = prefs.correlationMode,
                        rollingWindow = prefs.rollingWindow
                    )

                    _uiState.update { state ->
                        val clampedScrub = state.scrubIndex?.let { idx ->
                            if (analysis.points.isEmpty()) null
                            else idx.coerceIn(0, analysis.points.lastIndex)
                        }
                        state.copy(
                            feedState = MarketFeedState.Ready(analysis),
                            isRefreshing = false,
                            lastUpdatedMillis = System.currentTimeMillis(),
                            scrubIndex = clampedScrub
                        )
                    }

                    evaluateAndTriggerAlerts(analysis)
                },
                onFailure = { error ->
                    val previous = (_uiState.value.feedState as? MarketFeedState.Ready)?.analysis
                        ?: (_uiState.value.feedState as? MarketFeedState.Error)?.previousAnalysis
                    _uiState.update {
                        it.copy(
                            feedState = MarketFeedState.Error(
                                message = error.localizedMessage ?: "Impossibile recuperare le quotazioni",
                                previousAnalysis = previous
                            ),
                            isRefreshing = false
                        )
                    }
                }
            )
        }
    }

    private suspend fun evaluateAndTriggerAlerts(analysis: SynchronizedAnalysis) {
        val seriesB = analysis.seriesB ?: return
        val observed = analysis.latestRollingCorrelation ?: analysis.overallCorrelation ?: return
        val currentRules = _uiState.value.prefs.alertRules

        val (updatedRules, newEvents) = CorrelationEngine.evaluateAlerts(
            rules = currentRules,
            symbolA = analysis.seriesA.symbol,
            symbolB = seriesB.symbol,
            observedCorrelation = observed
        )

        if (newEvents.isNotEmpty()) {
            prefsRepository.saveAlertRules(updatedRules)
            prefsRepository.appendAlertHistory(newEvents)

            val latestEvent = newEvents.first()
            _uiState.update { it.copy(activeBannerAlert = latestEvent) }

            newEvents.forEach { event ->
                val matchingRule = updatedRules.find { it.id == event.ruleId }
                if (matchingRule?.systemNotification == true) {
                    notificationHelper.notifyAlert(event)
                }
            }
        }
    }

    fun setScrubIndex(index: Int?) {
        _uiState.update { it.copy(scrubIndex = index) }
    }

    fun toggleLivePolling() {
        _uiState.update {
            it.copy(
                isLivePolling = !it.isLivePolling,
                secondsUntilNextPoll = 5
            )
        }
    }

    fun toggleNormalizedPercentMode() {
        _uiState.update { it.copy(normalizedPercentMode = !it.normalizedPercentMode) }
    }

    fun toggleSecondChart() {
        viewModelScope.launch {
            val current = _uiState.value.prefs.showSecondChart
            prefsRepository.setShowSecondChart(!current)
        }
    }

    fun selectPair(symbolA: String, symbolB: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(scrubIndex = null) }
            prefsRepository.updateSymbols(symbolA, symbolB, showSecondChart = true)
        }
    }

    fun swapSymbols() {
        val prefs = _uiState.value.prefs
        if (!prefs.showSecondChart) return
        viewModelScope.launch {
            _uiState.update { it.copy(scrubIndex = null) }
            prefsRepository.updateSymbols(prefs.symbolB, prefs.symbolA, showSecondChart = true)
        }
    }

    fun selectTickerForSlot(slot: Int, symbol: String) {
        val prefs = _uiState.value.prefs
        val clean = symbol.trim().uppercase()
        viewModelScope.launch {
            _uiState.update { it.copy(scrubIndex = null) }
            if (slot == 1) {
                val nextB = if (clean == prefs.symbolB) prefs.symbolA else prefs.symbolB
                prefsRepository.updateSymbols(clean, nextB, showSecondChart = prefs.showSecondChart)
            } else {
                val nextA = if (clean == prefs.symbolA) prefs.symbolB else prefs.symbolA
                prefsRepository.updateSymbols(nextA, clean, showSecondChart = true)
            }
        }
    }

    fun setTimeframe(timeframe: TimeframeOption) {
        viewModelScope.launch {
            _uiState.update { it.copy(scrubIndex = null) }
            prefsRepository.updateTimeframe(timeframe)
        }
    }

    fun setCorrelationMode(mode: CorrelationMode) {
        viewModelScope.launch {
            prefsRepository.updateCorrelationMode(mode)
        }
    }

    fun setRollingWindow(window: Int) {
        viewModelScope.launch {
            prefsRepository.updateRollingWindow(window)
        }
    }

    fun toggleDarkTheme() {
        viewModelScope.launch {
            prefsRepository.setDarkTheme(!_uiState.value.prefs.darkTheme)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isBlank()) {
                _uiState.update {
                    it.copy(searchResults = marketRepository.quickTickers, isSearching = false)
                }
                return@launch
            }
            delay(280L)
            _uiState.update { it.copy(isSearching = true) }
            val res = marketRepository.searchTickers(query)
            _uiState.update { state ->
                state.copy(
                    searchResults = res.getOrDefault(emptyList()),
                    isSearching = false
                )
            }
        }
    }

    fun saveAlertRule(
        existingId: String?,
        symbolA: String,
        symbolB: String,
        applyToAllPairs: Boolean,
        condition: AlertCondition,
        threshold: Double,
        systemNotification: Boolean,
        note: String
    ) {
        viewModelScope.launch {
            val rules = _uiState.value.prefs.alertRules.toMutableList()
            val rule = CorrelationAlertRule(
                id = existingId ?: prefsRepository.newRuleId(),
                symbolA = symbolA.uppercase(),
                symbolB = symbolB.uppercase(),
                applyToAllPairs = applyToAllPairs,
                condition = condition,
                threshold = threshold,
                enabled = true,
                systemNotification = systemNotification,
                note = note.trim()
            )
            val idx = rules.indexOfFirst { it.id == rule.id }
            if (idx >= 0) {
                rules[idx] = rule
            } else {
                rules.add(0, rule)
            }
            prefsRepository.saveAlertRules(rules)

            val analysis = (_uiState.value.feedState as? MarketFeedState.Ready)?.analysis
            if (analysis != null) {
                evaluateAndTriggerAlerts(analysis)
            }
        }
    }

    fun toggleRuleEnabled(ruleId: String) {
        viewModelScope.launch {
            val updated = _uiState.value.prefs.alertRules.map {
                if (it.id == ruleId) it.copy(enabled = !it.enabled) else it
            }
            prefsRepository.saveAlertRules(updated)
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            val updated = _uiState.value.prefs.alertRules.filterNot { it.id == ruleId }
            prefsRepository.saveAlertRules(updated)
        }
    }

    fun clearAlertHistory() {
        viewModelScope.launch {
            prefsRepository.clearAlertHistory()
        }
    }

    fun dismissBannerAlert() {
        _uiState.update { it.copy(activeBannerAlert = null) }
    }

    fun hasNotificationPermission(): Boolean = notificationHelper.hasNotificationPermission()
}
