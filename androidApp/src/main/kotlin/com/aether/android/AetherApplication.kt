package com.aether.android

import android.app.Application
import com.aether.android.alarm.AlarmPreferences
import com.aether.android.alarm.AlarmScheduler
import com.aether.android.data.ApiKeyStore
import com.aether.android.data.GroqScheduleClient
import com.aether.android.data.OnDeviceLlm
import com.aether.core.data.AetherRepository
import com.aether.core.data.DatabaseDriverFactory
import com.aether.core.data.GoalsRepository
import com.aether.core.data.PersonalizationRepository
import com.aether.core.data.RoutineRepository
import com.aether.core.data.StudyRepository
import com.aether.core.db.AetherDatabase
import com.aether.core.engine.ContextEngine
import com.aether.core.engine.DigestEngine
import com.aether.core.engine.ExamEngine
import com.aether.core.engine.MindMapEngine
import com.aether.core.engine.PersonalizationEngine
import com.aether.core.engine.StudyEngine
import com.aether.core.engine.ScoringEngine

class AetherApplication : Application() {

    lateinit var repository: AetherRepository
        private set

    lateinit var goalsRepository: GoalsRepository
        private set

    lateinit var personalizationRepository: PersonalizationRepository
        private set

    lateinit var studyRepository: StudyRepository
        private set

    lateinit var onDeviceLlm: OnDeviceLlm
        private set

    lateinit var routineRepository: RoutineRepository
        private set

    lateinit var apiKeyStore: ApiKeyStore
        private set

    lateinit var alarmPreferences: AlarmPreferences
        private set

    lateinit var alarmScheduler: AlarmScheduler
        private set

    val scoringEngine = ScoringEngine()
    val contextEngine = ContextEngine()
    val personalizationEngine = PersonalizationEngine()
    val studyEngine = StudyEngine()
    val mindMapEngine = MindMapEngine()
    val digestEngine = DigestEngine(studyEngine)
    val examEngine = ExamEngine(studyEngine)
    val groqScheduleClient = GroqScheduleClient()

    /** A loaded LLM holds hundreds of MB; give it back as soon as the app is in the background and memory is tight. */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_BACKGROUND && ::onDeviceLlm.isInitialized) onDeviceLlm.unload()
    }

    override fun onCreate() {
        super.onCreate()
        val database = AetherDatabase(DatabaseDriverFactory(this).createDriver())
        repository = AetherRepository(database)
        goalsRepository = GoalsRepository(database)
        personalizationRepository = PersonalizationRepository(database)
        studyRepository = StudyRepository(database)
        routineRepository = RoutineRepository(database)
        apiKeyStore = ApiKeyStore(this)
        onDeviceLlm = OnDeviceLlm(this)
        alarmPreferences = AlarmPreferences(this)
        alarmScheduler = AlarmScheduler(this)
    }
}
