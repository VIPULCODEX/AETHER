package com.aether.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.aether.android.alarm.AlarmPreferences
import com.aether.android.alarm.AlarmScheduler
import com.aether.android.data.ApiKeyStore
import com.aether.android.data.GroqScheduleClient
import com.aether.android.data.OnDeviceLlm
import com.aether.android.ui.dashboard.DashboardViewModel
import com.aether.android.ui.goals.GoalsViewModel
import com.aether.android.ui.gym.GymViewModel
import com.aether.android.ui.journal.JournalViewModel
import com.aether.android.ui.mindmap.LifeMapViewModel
import com.aether.android.ui.personalize.PersonalizeViewModel
import com.aether.android.ui.routines.RoutinesViewModel
import com.aether.android.ui.research.ResearchViewModel
import com.aether.android.ui.settings.SettingsViewModel
import com.aether.android.ui.study.StudyViewModel
import com.aether.core.data.AetherRepository
import com.aether.core.data.GoalsRepository
import com.aether.core.data.PersonalizationRepository
import com.aether.core.data.RoutineRepository
import com.aether.core.data.StudyRepository
import com.aether.core.engine.ContextEngine
import com.aether.core.engine.DigestEngine
import com.aether.core.engine.ExamEngine
import com.aether.core.engine.MindMapEngine
import com.aether.core.engine.PersonalizationEngine
import com.aether.core.engine.StudyEngine
import com.aether.core.engine.ScoringEngine

class AetherViewModelFactory(
    private val repository: AetherRepository,
    private val goalsRepository: GoalsRepository,
    private val scoringEngine: ScoringEngine,
    private val contextEngine: ContextEngine,
    private val personalizationRepository: PersonalizationRepository,
    private val personalizationEngine: PersonalizationEngine,
    private val studyRepository: StudyRepository,
    private val routineRepository: RoutineRepository,
    private val studyEngine: StudyEngine,
    private val mindMapEngine: MindMapEngine,
    private val onDeviceLlm: OnDeviceLlm,
    private val digestEngine: DigestEngine,
    private val examEngine: ExamEngine,
    private val apiKeyStore: ApiKeyStore,
    private val groqScheduleClient: GroqScheduleClient,
    private val claudeNotesClient: com.aether.android.data.ClaudeNotesClient,
    private val alarmPreferences: AlarmPreferences,
    private val alarmScheduler: AlarmScheduler
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when (modelClass) {
            DashboardViewModel::class.java ->
                DashboardViewModel(repository, goalsRepository, scoringEngine, contextEngine, personalizationRepository, personalizationEngine, routineRepository, studyRepository) as T

            JournalViewModel::class.java ->
                JournalViewModel(repository) as T

            GoalsViewModel::class.java ->
                GoalsViewModel(repository, goalsRepository, apiKeyStore, groqScheduleClient, alarmScheduler) as T

            GymViewModel::class.java ->
                GymViewModel(repository) as T

            SettingsViewModel::class.java ->
                SettingsViewModel(apiKeyStore, alarmPreferences, alarmScheduler, repository) as T

            ResearchViewModel::class.java ->
                ResearchViewModel(repository) as T

            PersonalizeViewModel::class.java ->
                PersonalizeViewModel(repository, goalsRepository, personalizationRepository, personalizationEngine, routineRepository) as T

            StudyViewModel::class.java ->
                StudyViewModel(studyRepository, studyEngine, mindMapEngine, onDeviceLlm, digestEngine, examEngine, personalizationRepository, apiKeyStore, claudeNotesClient) as T

            LifeMapViewModel::class.java ->
                LifeMapViewModel(goalsRepository, mindMapEngine) as T

            RoutinesViewModel::class.java ->
                RoutinesViewModel(routineRepository, repository, goalsRepository, personalizationRepository, personalizationEngine, alarmScheduler) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
    }
}
