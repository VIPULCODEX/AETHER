package com.aether.android

import android.app.Application
import com.aether.android.alarm.AlarmPreferences
import com.aether.android.alarm.AlarmScheduler
import com.aether.android.data.ApiKeyStore
import com.aether.android.data.GroqScheduleClient
import com.aether.core.data.AetherRepository
import com.aether.core.data.DatabaseDriverFactory
import com.aether.core.data.GoalsRepository
import com.aether.core.db.AetherDatabase
import com.aether.core.engine.ContextEngine
import com.aether.core.engine.ScoringEngine

class AetherApplication : Application() {

    lateinit var repository: AetherRepository
        private set

    lateinit var goalsRepository: GoalsRepository
        private set

    lateinit var apiKeyStore: ApiKeyStore
        private set

    lateinit var alarmPreferences: AlarmPreferences
        private set

    lateinit var alarmScheduler: AlarmScheduler
        private set

    val scoringEngine = ScoringEngine()
    val contextEngine = ContextEngine()
    val groqScheduleClient = GroqScheduleClient()

    override fun onCreate() {
        super.onCreate()
        val database = AetherDatabase(DatabaseDriverFactory(this).createDriver())
        repository = AetherRepository(database)
        goalsRepository = GoalsRepository(database)
        apiKeyStore = ApiKeyStore(this)
        alarmPreferences = AlarmPreferences(this)
        alarmScheduler = AlarmScheduler(this)
    }
}
