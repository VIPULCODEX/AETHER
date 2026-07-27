package com.aether.android

import android.app.Application
import com.aether.core.data.AetherRepository
import com.aether.core.data.DatabaseDriverFactory
import com.aether.core.db.AetherDatabase
import com.aether.core.engine.ContextEngine
import com.aether.core.engine.ScoringEngine

class AetherApplication : Application() {

    lateinit var repository: AetherRepository
        private set

    val scoringEngine = ScoringEngine()
    val contextEngine = ContextEngine()

    override fun onCreate() {
        super.onCreate()
        val driver = DatabaseDriverFactory(this).createDriver()
        repository = AetherRepository(AetherDatabase(driver))
    }
}
