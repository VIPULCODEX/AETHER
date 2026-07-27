package com.aether.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aether.android.AetherViewModelFactory
import com.aether.android.data.ApiKeyStore
import com.aether.android.data.GroqScheduleClient
import com.aether.android.ui.common.ComingSoonScreen
import com.aether.android.ui.common.MoreScreen
import com.aether.android.ui.dashboard.DashboardScreen
import com.aether.android.ui.dashboard.DashboardViewModel
import com.aether.android.ui.goals.GoalsScreen
import com.aether.android.ui.goals.GoalsViewModel
import com.aether.android.ui.gym.GymScreen
import com.aether.android.ui.gym.GymViewModel
import com.aether.android.ui.journal.JournalScreen
import com.aether.android.ui.journal.JournalViewModel
import com.aether.android.ui.research.ResearchScreen
import com.aether.android.ui.research.ResearchViewModel
import com.aether.android.ui.settings.SettingsScreen
import com.aether.android.ui.settings.SettingsViewModel
import com.aether.core.data.AetherRepository
import com.aether.core.data.GoalsRepository
import com.aether.core.engine.ContextEngine
import com.aether.core.engine.ScoringEngine

private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_JOURNAL = "journal"
private const val ROUTE_GOALS = "goals"
private const val ROUTE_GYM = "gym"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_MORE = "more"
private const val ROUTE_RESEARCH = "research"
private const val ROUTE_COMING_SOON = "coming_soon/{slug}"

@Composable
fun AetherNavHost(
    repository: AetherRepository,
    goalsRepository: GoalsRepository,
    scoringEngine: ScoringEngine,
    contextEngine: ContextEngine,
    apiKeyStore: ApiKeyStore,
    groqScheduleClient: GroqScheduleClient
) {
    val navController = rememberNavController()
    val factory = remember {
        AetherViewModelFactory(repository, goalsRepository, scoringEngine, contextEngine, apiKeyStore, groqScheduleClient)
    }

    val navigate: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = ROUTE_DASHBOARD) {
        composable(ROUTE_DASHBOARD) {
            val viewModel: DashboardViewModel = viewModel(factory = factory)
            DashboardScreen(viewModel = viewModel, onNavigate = navigate)
        }
        composable(ROUTE_JOURNAL) {
            val viewModel: JournalViewModel = viewModel(factory = factory)
            JournalScreen(viewModel = viewModel, onNavigate = navigate)
        }
        composable(ROUTE_GOALS) {
            val viewModel: GoalsViewModel = viewModel(factory = factory)
            GoalsScreen(viewModel = viewModel, onNavigate = navigate)
        }
        composable(ROUTE_GYM) {
            val viewModel: GymViewModel = viewModel(factory = factory)
            GymScreen(viewModel = viewModel, onNavigate = navigate)
        }
        composable(ROUTE_SETTINGS) {
            val viewModel: SettingsViewModel = viewModel(factory = factory)
            SettingsScreen(viewModel = viewModel, onNavigate = navigate)
        }
        composable(ROUTE_MORE) {
            MoreScreen(onNavigate = navigate)
        }
        composable(ROUTE_RESEARCH) {
            val viewModel: ResearchViewModel = viewModel(factory = factory)
            ResearchScreen(viewModel = viewModel, onNavigate = navigate)
        }
        composable(
            route = ROUTE_COMING_SOON,
            arguments = listOf(navArgument("slug") { type = NavType.StringType })
        ) { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("slug") ?: ""
            ComingSoonScreen(slug = slug, onNavigate = navigate)
        }
    }
}
