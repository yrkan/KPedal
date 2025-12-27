package io.github.kpedal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.kpedal.drill.model.DrillExecutionStatus
import io.github.kpedal.ui.screens.AlertSettingsScreen
import io.github.kpedal.ui.screens.HistoryScreen
import io.github.kpedal.ui.screens.HomeScreen
import io.github.kpedal.ui.screens.RideDetailScreen
import io.github.kpedal.ui.screens.SettingsScreen
import io.github.kpedal.ui.screens.LiveScreen
import io.github.kpedal.ui.screens.drills.CustomDrillScreen
import io.github.kpedal.ui.screens.drills.DrillDetailScreen
import io.github.kpedal.ui.screens.drills.DrillExecutionScreen
import io.github.kpedal.ui.screens.drills.DrillHistoryScreen
import io.github.kpedal.ui.screens.drills.DrillResultScreen
import io.github.kpedal.ui.screens.drills.DrillsListScreen
import io.github.kpedal.ui.screens.layouts.*
import io.github.kpedal.ui.screens.analytics.AnalyticsScreen
import io.github.kpedal.ui.screens.help.CloudSyncHelpScreen
import io.github.kpedal.ui.screens.help.HelpScreen
import io.github.kpedal.ui.screens.help.OnboardingScreen
import io.github.kpedal.ui.screens.AchievementsScreen
import io.github.kpedal.ui.screens.PedalInfoScreen
import io.github.kpedal.ui.screens.LayoutsScreen
import io.github.kpedal.ui.screens.ChallengesScreen
import io.github.kpedal.data.models.TrendData
import io.github.kpedal.ui.theme.KPedalTheme
import kotlinx.coroutines.flow.first
import io.github.kpedal.ui.theme.Theme
import io.github.kpedal.util.LocaleHelper

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        // Wrap context with selected locale before activity is created
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KPedalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Theme.colors.background
                ) {
                    KPedalApp()
                }
            }
        }
    }
}

@Composable
fun KPedalApp(
    viewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsState()

    // Navigate to onboarding if not seen
    LaunchedEffect(hasSeenOnboarding) {
        if (!hasSeenOnboarding) {
            navController.navigate("onboarding") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        // Home - Navigation menu with dashboard stats
        composable("home") {
            val dashboardData by viewModel.dashboardData.collectAsState()
            val unlockedAchievements by viewModel.unlockedAchievements.collectAsState()
            HomeScreen(
                dashboardData = dashboardData,
                unlockedAchievements = unlockedAchievements,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        // Achievements screen
        composable("achievements") {
            val unlockedAchievements by viewModel.unlockedAchievements.collectAsState()
            AchievementsScreen(
                unlockedAchievements = unlockedAchievements,
                onBack = { navController.popBackStack() }
            )
        }

        // Challenges screen
        composable("challenges") {
            val progress by viewModel.challengeProgress.collectAsState()
            ChallengesScreen(
                progress = progress,
                onBack = { navController.popBackStack() }
            )
        }

        // Pedal Info screen
        composable("pedal-info") {
            val pedalInfo by viewModel.pedalInfo.collectAsState()
            val metrics by viewModel.metrics.collectAsState()
            PedalInfoScreen(
                pedalInfo = pedalInfo,
                metrics = metrics,
                onBack = { navController.popBackStack() }
            )
        }

        // Layouts list
        composable("layouts") {
            LayoutsScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        // Layout 1: Quick Glance
        composable("quick-glance") {
            val metrics by viewModel.metrics.collectAsState()
            QuickGlanceScreen(
                metrics = metrics,
                onBack = { navController.popBackStack() }
            )
        }

        // Layout 2: Power + Balance
        composable("power-balance") {
            val metrics by viewModel.metrics.collectAsState()
            PowerBalanceScreen(
                metrics = metrics,
                onBack = { navController.popBackStack() }
            )
        }

        // Layout 3: Efficiency
        composable("efficiency") {
            val metrics by viewModel.metrics.collectAsState()
            EfficiencyScreen(
                metrics = metrics,
                onBack = { navController.popBackStack() }
            )
        }

        // Layout 4: Full Overview
        composable("full-overview") {
            val metrics by viewModel.metrics.collectAsState()
            FullOverviewScreen(
                metrics = metrics,
                onBack = { navController.popBackStack() }
            )
        }

        // Layout 5: Balance Trend
        composable("balance-trend") {
            val metrics by viewModel.metrics.collectAsState()
            BalanceTrendScreen(
                metrics = metrics,
                onBack = { navController.popBackStack() }
            )
        }

        // Layout 6: Single Balance
        composable("single-balance") {
            val metrics by viewModel.metrics.collectAsState()
            SingleBalanceScreen(
                metrics = metrics,
                onBack = { navController.popBackStack() }
            )
        }

        // Settings (collects only when active)
        composable("settings") {
            val context = androidx.compose.ui.platform.LocalContext.current
            val settings by viewModel.settings.collectAsState()
            val authState by viewModel.authState.collectAsState()
            val syncState by viewModel.syncState.collectAsState()
            val deviceAuthState by viewModel.deviceAuthState.collectAsState()
            val backgroundModeEnabled by viewModel.backgroundModeEnabled.collectAsState()
            val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsState()
            val currentLanguage by viewModel.currentLanguage.collectAsState()

            SettingsScreen(
                settings = settings,
                authState = authState,
                syncState = syncState,
                deviceAuthState = deviceAuthState,
                backgroundModeEnabled = backgroundModeEnabled,
                autoSyncEnabled = autoSyncEnabled,
                currentLanguage = currentLanguage,
                onBack = { navController.popBackStack() },
                onNavigateToAlertSettings = { navController.navigate("alert-settings") },
                onUpdateBalanceThreshold = viewModel::updateBalanceThreshold,
                onUpdateTeOptimalRange = viewModel::updateTeOptimalRange,
                onUpdatePsMinimum = viewModel::updatePsMinimum,
                onStartDeviceAuth = viewModel::startDeviceAuth,
                onCancelDeviceAuth = viewModel::cancelDeviceAuth,
                onSignOut = viewModel::signOut,
                onManualSync = viewModel::triggerManualSync,
                onFullSync = viewModel::triggerFullSync,
                onUpdateBackgroundMode = viewModel::updateBackgroundModeEnabled,
                onUpdateAutoSync = viewModel::updateAutoSyncEnabled,
                onUpdateLanguage = { language ->
                    if (viewModel.updateLanguage(language)) {
                        // Delay for smooth transition, then recreate activity
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            (context as? android.app.Activity)?.recreate()
                        }, 200)
                    }
                },
                onCheckSyncRequest = viewModel::checkForSyncRequest,
                onDeviceRevokedAcknowledged = viewModel::clearDeviceRevokedFlag,
                onNavigateToLinkHelp = { navController.navigate("cloud-sync-help") }
            )
        }

        // Cloud Sync Help
        composable("cloud-sync-help") {
            CloudSyncHelpScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Alert Settings
        composable("alert-settings") {
            val alertSettings by viewModel.alertSettings.collectAsState()
            AlertSettingsScreen(
                alertSettings = alertSettings,
                onBack = { navController.popBackStack() },
                onUpdateGlobalEnabled = viewModel::updateGlobalAlertsEnabled,
                onUpdateScreenWake = viewModel::updateScreenWakeOnAlert,
                onUpdateBalanceConfig = viewModel::updateBalanceAlertConfig,
                onUpdateTeConfig = viewModel::updateTeAlertConfig,
                onUpdatePsConfig = viewModel::updatePsAlertConfig
            )
        }

        // Live (collects only when active)
        composable("live") {
            val liveData by viewModel.liveData.collectAsState()
            LiveScreen(
                liveData = liveData,
                onBack = { navController.popBackStack() },
                onSave = { viewModel.manualSaveRide() },
                onNavigateToDrills = {
                    navController.navigate("drills") {
                        popUpTo("live") { inclusive = true }
                    }
                }
            )
        }

        // History - List of saved rides
        composable("history") {
            val rides by viewModel.rides.collectAsState()
            HistoryScreen(
                rides = rides,
                onBack = { navController.popBackStack() },
                onRideClick = { id -> navController.navigate("ride-detail/$id") },
                onDeleteRide = { id -> viewModel.deleteRide(id) }
            )
        }

        // Analytics - Trends and progress
        composable("analytics") {
            val trendData by viewModel.trendData.collectAsState()

            // Load initial data
            LaunchedEffect(Unit) {
                viewModel.loadTrendData(TrendData.Period.WEEK)
            }

            AnalyticsScreen(
                trendData = trendData,
                onBack = { navController.popBackStack() },
                onPeriodChange = { period -> viewModel.loadTrendData(period) }
            )
        }

        // Help - Quick reference
        composable("help") {
            HelpScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Onboarding - First run tutorial
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    viewModel.completeOnboarding()
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        // Ride Detail - Single ride view
        composable(
            route = "ride-detail/{rideId}",
            arguments = listOf(navArgument("rideId") { type = NavType.LongType })
        ) { backStackEntry ->
            val rideId = backStackEntry.arguments?.getLong("rideId") ?: return@composable
            val ride by viewModel.getRideFlow(rideId).collectAsState(initial = null)
            ride?.let {
                RideDetailScreen(
                    ride = it,
                    onBack = { navController.popBackStack() },
                    onDelete = {
                        viewModel.deleteRide(it.id)
                        navController.popBackStack()
                    },
                    onRatingChange = { rating ->
                        viewModel.updateRideRating(it.id, rating)
                    }
                )
            }
        }

        // ========== Drills ==========

        // Drills - List of available drills
        composable("drills") {
            val customDrills by viewModel.customDrills.collectAsState()
            DrillsListScreen(
                customDrills = customDrills,
                onBack = { navController.popBackStack() },
                onDrillClick = { drillId -> navController.navigate("drill-detail/$drillId") },
                onHistoryClick = { navController.navigate("drill-history") },
                onCreateCustomDrill = { navController.navigate("custom-drill") },
                onDeleteCustomDrill = { drillId -> viewModel.deleteCustomDrill(drillId) },
                onNavigateToLive = {
                    navController.navigate("live") {
                        popUpTo("drills") { inclusive = true }
                    }
                }
            )
        }

        // Custom Drill - Create/Edit
        composable("custom-drill") {
            CustomDrillScreen(
                existingDrill = null,
                onSave = { entity ->
                    viewModel.saveCustomDrill(entity)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Drill History - Past results and statistics
        composable("drill-history") {
            val drillResults by viewModel.drillResults.collectAsState()
            DrillHistoryScreen(
                results = drillResults,
                onBack = { navController.popBackStack() },
                onResultClick = { /* Could navigate to detail view */ },
                onDeleteResult = { id -> viewModel.deleteDrillResult(id) },
                onDeleteAll = { viewModel.deleteAllDrillResults() }
            )
        }

        // Drill Detail - Pre-start view
        composable(
            route = "drill-detail/{drillId}",
            arguments = listOf(navArgument("drillId") { type = NavType.StringType })
        ) { backStackEntry ->
            val drillId = backStackEntry.arguments?.getString("drillId") ?: return@composable
            val drill = viewModel.getDrill(drillId) ?: return@composable
            val bestScore by viewModel.getBestScore(drillId).collectAsState(initial = null)
            val completedCount by viewModel.getCompletedCount(drillId).collectAsState(initial = 0)

            val drillName = drill.nameOverride ?: stringResource(drill.nameRes)
            DrillDetailScreen(
                drill = drill,
                bestScore = bestScore,
                completedCount = completedCount,
                onBack = { navController.popBackStack() },
                onStart = {
                    viewModel.startDrill(drill, drillName)
                    navController.navigate("drill-execution/$drillId") {
                        popUpTo("drill-detail/$drillId") { inclusive = true }
                    }
                }
            )
        }

        // Drill Execution - Active drill
        composable(
            route = "drill-execution/{drillId}",
            arguments = listOf(navArgument("drillId") { type = NavType.StringType })
        ) { backStackEntry ->
            val drillId = backStackEntry.arguments?.getString("drillId") ?: return@composable
            val drillState by viewModel.drillState.collectAsState()
            val metrics by viewModel.metrics.collectAsState()

            // Navigate to result when drill completes or is cancelled
            LaunchedEffect(drillState?.status) {
                val status = drillState?.status
                if (status == DrillExecutionStatus.COMPLETED || status == DrillExecutionStatus.CANCELLED) {
                    navController.navigate("drill-result/$drillId") {
                        popUpTo("drill-execution/$drillId") { inclusive = true }
                    }
                }
            }

            drillState?.let { state ->
                if (state.status != DrillExecutionStatus.COMPLETED && state.status != DrillExecutionStatus.CANCELLED) {
                    DrillExecutionScreen(
                        state = state,
                        metrics = metrics,
                        onPause = { viewModel.pauseDrill() },
                        onResume = { viewModel.resumeDrill() },
                        onStop = { viewModel.stopDrill() }
                    )
                }
            } ?: run {
                // Show loading while state initializes
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Text("Loading...", color = Theme.colors.dim)
                }
            }
        }

        // Drill Result - Completion screen
        composable(
            route = "drill-result/{drillId}",
            arguments = listOf(navArgument("drillId") { type = NavType.StringType })
        ) { backStackEntry ->
            val drillId = backStackEntry.arguments?.getString("drillId") ?: return@composable
            val drillResult by viewModel.drillResult.collectAsState()

            // Collect best score for comparison
            val bestScore by viewModel.getBestScore(drillId).collectAsState(initial = null)

            // Timeout to navigate away if result never arrives
            LaunchedEffect(Unit) {
                // Wait for result using flow, with 5 second timeout
                val result = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                    viewModel.drillResult.first { it != null }
                }
                // If timeout and still no result, go back
                if (result == null) {
                    android.util.Log.w("MainActivity", "Drill result timeout, navigating to drills")
                    navController.navigate("drills") {
                        popUpTo("drills") { inclusive = true }
                    }
                }
            }

            drillResult?.let { result ->
                // Pre-resolve drill name for retry (using the name stored in result)
                val retryDrillName = result.drillName
                DrillResultScreen(
                    result = result,
                    previousBestScore = bestScore,
                    onDone = {
                        viewModel.clearDrillResult()
                        navController.navigate("drills") {
                            popUpTo("drills") { inclusive = true }
                        }
                    },
                    onRetry = {
                        viewModel.getDrill(drillId)?.let { drill ->
                            viewModel.startDrill(drill, retryDrillName)
                            navController.navigate("drill-execution/$drillId") {
                                popUpTo("drill-result/$drillId") { inclusive = true }
                            }
                        }
                    }
                )
            } ?: run {
                // Show loading while waiting for result
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Text("Loading result...", color = Theme.colors.dim)
                }
            }
        }
    }
}
