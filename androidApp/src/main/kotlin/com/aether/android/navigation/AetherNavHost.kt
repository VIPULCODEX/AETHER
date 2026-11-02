package com.aether.android.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aether.android.AetherViewModelFactory
import com.aether.android.alarm.AlarmPreferences
import com.aether.android.alarm.AlarmScheduler
import com.aether.android.data.ApiKeyStore
import com.aether.android.data.GroqScheduleClient
import com.aether.android.data.OnDeviceLlm
import com.aether.android.ui.common.ComingSoonScreen
import com.aether.android.ui.components.AetherAppChrome
import com.aether.android.ui.common.MoreScreen
import com.aether.android.ui.dashboard.DashboardScreen
import com.aether.android.ui.dashboard.DashboardViewModel
import com.aether.android.ui.goals.GoalsScreen
import com.aether.android.ui.goals.GoalsViewModel
import com.aether.android.ui.gym.GymScreen
import com.aether.android.ui.gym.GymViewModel
import com.aether.android.ui.journal.JournalScreen
import com.aether.android.ui.journal.JournalViewModel
import com.aether.android.ui.mindmap.LifeMapScreen
import com.aether.android.ui.mindmap.LifeMapViewModel
import com.aether.android.ui.personalize.PersonalizeScreen
import com.aether.android.ui.personalize.PersonalizeViewModel
import com.aether.android.ui.research.ResearchScreen
import com.aether.android.ui.research.ResearchViewModel
import com.aether.android.ui.settings.SettingsScreen
import com.aether.android.ui.settings.SettingsViewModel
import com.aether.android.ui.study.StudyScreen
import com.aether.android.ui.study.StudyViewModel
import com.aether.core.data.AetherRepository
import com.aether.core.data.GoalsRepository
import com.aether.core.data.PersonalizationRepository
import com.aether.core.data.StudyRepository
import com.aether.core.engine.ContextEngine
import com.aether.core.engine.DigestEngine
import com.aether.core.engine.ExamEngine
import com.aether.core.engine.MindMapEngine
import com.aether.core.engine.PersonalizationEngine
import com.aether.core.engine.StudyEngine
import com.aether.core.engine.ScoringEngine

private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_JOURNAL = "journal"
private const val ROUTE_GOALS = "goals"
private const val ROUTE_GYM = "gym"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_MORE = "more"
private const val ROUTE_RESEARCH = "research"
private const val ROUTE_PERSONALIZE = "personalize"
private const val ROUTE_STUDY = "study"
private const val ROUTE_LIFE_MAP = "lifemap"
private const val ROUTE_COMING_SOON = "coming_soon/{slug}"

@Composable
fun AetherNavHost(
    repository: AetherRepository,
    goalsRepository: GoalsRepository,
    scoringEngine: ScoringEngine,
    contextEngine: ContextEngine,
    personalizationRepository: PersonalizationRepository,
    personalizationEngine: PersonalizationEngine,
    studyRepository: StudyRepository,
    studyEngine: StudyEngine,
    mindMapEngine: MindMapEngine,
    onDeviceLlm: OnDeviceLlm,
    digestEngine: DigestEngine,
    examEngine: ExamEngine,
    apiKeyStore: ApiKeyStore,
    groqScheduleClient: GroqScheduleClient,
    alarmPreferences: AlarmPreferences,
    alarmScheduler: AlarmScheduler
) {
    val navController = rememberNavController()
    val factory = remember {
        AetherViewModelFactory(
            repository, goalsRepository, scoringEngine, contextEngine,
            personalizationRepository, personalizationEngine, studyRepository, studyEngine, mindMapEngine, onDeviceLlm, digestEngine, examEngine, apiKeyStore, groqScheduleClient, alarmPreferences, alarmScheduler
        )
    }

    val navigate: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val sharedText by com.aether.android.data.ShareInbox.pending.collectAsState()
    LaunchedEffect(sharedText) {
        if (sharedText != null) navigate(ROUTE_STUDY)
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = when (backStackEntry?.destination?.route) {
        ROUTE_COMING_SOON -> "coming_soon/${backStackEntry?.arguments?.getString("slug").orEmpty()}"
        else -> backStackEntry?.destination?.route ?: ROUTE_DASHBOARD
    }

    AetherAppChrome(currentRoute = currentRoute, onNavigate = navigate) {
        NavHost(
            navController = navController,
            startDestination = ROUTE_DASHBOARD,
            enterTransition = { fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 30 } },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 30 } },
            popExitTransition = { fadeOut(tween(120)) }
        ) {
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
        composable(ROUTE_PERSONALIZE) {
            val viewModel: PersonalizeViewModel = viewModel(factory = factory)
            PersonalizeScreen(viewModel = viewModel)
        }
        composable(ROUTE_LIFE_MAP) {
            val viewModel: LifeMapViewModel = viewModel(factory = factory)
            LifeMapScreen(viewModel = viewModel)
        }
        composable(ROUTE_STUDY) {
            val viewModel: StudyViewModel = viewModel(factory = factory)
            StudyScreen(viewModel = viewModel)
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
}
