package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.model.CorrelationAlertRule
import com.example.ui.MainViewModel
import com.example.ui.MarketFeedState
import com.example.ui.components.CreateAlertSheet
import com.example.ui.components.TickerPickerSheet
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.theme.CorrelaPulseTheme

private const val ROUTE_TERMINAL = "terminal"
private const val ROUTE_ALERTS = "alerts"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            CorrelaPulseTheme(darkTheme = uiState.prefs.darkTheme) {
                CorrelaPulseApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun CorrelaPulseApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var tickerPickerSlot by remember { mutableStateOf<Int?>(null) }
    var showCreateAlertSheet by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<CorrelationAlertRule?>(null) }

    var hasNotifPermission by remember { mutableStateOf(viewModel.hasNotificationPermission()) }
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted || viewModel.hasNotificationPermission()
    }

    val requestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val enabledAlertCount = uiState.prefs.alertRules.count { it.enabled }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 840.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        val isTerminal = currentDestination?.hierarchy?.any { it.route == ROUTE_TERMINAL } == true
                        val isAlerts = currentDestination?.hierarchy?.any { it.route == ROUTE_ALERTS } == true

                        NavigationBarItem(
                            selected = isTerminal,
                            onClick = { navigateTo(ROUTE_TERMINAL) },
                            icon = {
                                Icon(
                                    imageVector = if (isTerminal) Icons.Filled.ShowChart else Icons.Outlined.ShowChart,
                                    contentDescription = stringResource(R.string.tab_terminal)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_terminal)) },
                            modifier = Modifier.testTag("nav_tab_terminal")
                        )

                        NavigationBarItem(
                            selected = isAlerts,
                            onClick = { navigateTo(ROUTE_ALERTS) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (enabledAlertCount > 0) {
                                            Badge { Text("$enabledAlertCount") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isAlerts) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsActive,
                                        contentDescription = stringResource(R.string.tab_alerts)
                                    )
                                }
                            },
                            label = { Text(stringResource(R.string.tab_alerts)) },
                            modifier = Modifier.testTag("nav_tab_alerts")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    val isTerminal = currentDestination?.hierarchy?.any { it.route == ROUTE_TERMINAL } == true
                    val isAlerts = currentDestination?.hierarchy?.any { it.route == ROUTE_ALERTS } == true

                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        NavigationRailItem(
                            selected = isTerminal,
                            onClick = { navigateTo(ROUTE_TERMINAL) },
                            icon = {
                                Icon(
                                    imageVector = if (isTerminal) Icons.Filled.ShowChart else Icons.Outlined.ShowChart,
                                    contentDescription = stringResource(R.string.tab_terminal)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_terminal)) }
                        )
                        NavigationRailItem(
                            selected = isAlerts,
                            onClick = { navigateTo(ROUTE_ALERTS) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (enabledAlertCount > 0) {
                                            Badge { Text("$enabledAlertCount") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isAlerts) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsActive,
                                        contentDescription = stringResource(R.string.tab_alerts)
                                    )
                                }
                            },
                            label = { Text(stringResource(R.string.tab_alerts)) }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_TERMINAL
                    ) {
                        composable(ROUTE_TERMINAL) {
                            TerminalScreen(
                                uiState = uiState,
                                curatedPairs = viewModel.curatedPairs,
                                onToggleLivePolling = viewModel::toggleLivePolling,
                                onRefreshNow = viewModel::refreshNow,
                                onToggleDarkTheme = viewModel::toggleDarkTheme,
                                onSelectPair = viewModel::selectPair,
                                onTimeframeChange = viewModel::setTimeframe,
                                onSwapSymbols = viewModel::swapSymbols,
                                onToggleNormalizedMode = viewModel::toggleNormalizedPercentMode,
                                onToggleSecondChart = viewModel::toggleSecondChart,
                                onScrubChange = viewModel::setScrubIndex,
                                onOpenTickerPicker = { slot ->
                                    viewModel.updateSearchQuery("")
                                    tickerPickerSlot = slot
                                },
                                onCorrelationModeChange = viewModel::setCorrelationMode,
                                onRollingWindowChange = viewModel::setRollingWindow,
                                onOpenCreateAlert = {
                                    editingRule = null
                                    showCreateAlertSheet = true
                                },
                                onNavigateToAlerts = { navigateTo(ROUTE_ALERTS) },
                                onDismissBanner = viewModel::dismissBannerAlert
                            )
                        }

                        composable(ROUTE_ALERTS) {
                            AlertsScreen(
                                uiState = uiState,
                                hasNotificationPermission = hasNotifPermission,
                                onRequestNotificationPermission = requestNotificationPermission,
                                onAddAlertClick = {
                                    editingRule = null
                                    showCreateAlertSheet = true
                                },
                                onEditRule = { rule ->
                                    editingRule = rule
                                    showCreateAlertSheet = true
                                },
                                onToggleRule = viewModel::toggleRuleEnabled,
                                onDeleteRule = viewModel::deleteRule,
                                onClearHistory = viewModel::clearAlertHistory
                            )
                        }
                    }
                }
            }
        }
    }

    tickerPickerSlot?.let { slot ->
        val currentSymbol = if (slot == 1) uiState.prefs.symbolA else uiState.prefs.symbolB
        TickerPickerSheet(
            slotNumber = slot,
            currentSymbol = currentSymbol,
            searchQuery = uiState.searchQuery,
            searchResults = uiState.searchResults,
            isSearching = uiState.isSearching,
            onQueryChange = viewModel::updateSearchQuery,
            onSelectTicker = { symbol ->
                viewModel.selectTickerForSlot(slot, symbol)
            },
            onDismiss = { tickerPickerSlot = null }
        )
    }

    if (showCreateAlertSheet) {
        val currentAnalysis = (uiState.feedState as? MarketFeedState.Ready)?.analysis
        val currentCorr = currentAnalysis?.latestRollingCorrelation ?: currentAnalysis?.overallCorrelation
        CreateAlertSheet(
            symbolA = uiState.prefs.symbolA,
            symbolB = uiState.prefs.symbolB,
            currentCorrelation = currentCorr,
            editingRule = editingRule,
            onRequestNotificationPermission = requestNotificationPermission,
            onSave = viewModel::saveAlertRule,
            onDismiss = {
                showCreateAlertSheet = false
                editingRule = null
            }
        )
    }
}
