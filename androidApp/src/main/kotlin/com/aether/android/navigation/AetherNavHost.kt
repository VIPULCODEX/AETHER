package com.aether.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aether.android.AetherViewModelFactory
import com.aether.android.ui.dashboard.DashboardScreen
import com.aether.android.ui.dashboard.DashboardViewModel
import com.aether.android.ui.journal.JournalScreen
import com.aether.android.ui.journal.JournalViewModel
import com.aether.core.data.AetherRepository
import com.aether.core.engine.ContextEngine
import com.aether.core.engine.ScoringEngine

private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_JOURNAL = "journal"

@Composable
fun AetherNavHost(
    repository: AetherRepository,
    scoringEngine: ScoringEngine,
    contextEngine: ContextEngine
) {
    val navController = rememberNavController()
    val factory = remember { AetherViewModelFactory(repository, scoringEngine, contextEngine) }

    NavHost(navController = navController, startDestination = ROUTE_DASHBOARD) {
        composable(ROUTE_DASHBOARD) {
            val viewModel: DashboardViewModel = viewModel(factory = factory)
            DashboardScreen(
                viewModel = viewModel,
                onOpenJournal = { navController.navigate(ROUTE_JOURNAL) }
            )
        }
        composable(ROUTE_JOURNAL) {
            val viewModel: JournalViewModel = viewModel(factory = factory)
            JournalScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
