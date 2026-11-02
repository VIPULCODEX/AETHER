package com.aether.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.aether.core.db.AetherDatabase
import com.aether.core.model.BehaviorEvent
import com.aether.core.model.BehaviorEventType
import com.aether.core.model.Chronotype
import com.aether.core.model.CoachTone
import com.aether.core.model.Persona
import com.aether.core.model.PersonalizationPrefs
import com.aether.core.model.PlanningStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Explicit preferences + the behavior log that feeds on-device learning. Nothing here leaves the device. */
class PersonalizationRepository(private val database: AetherDatabase) {

    fun observePrefs(): Flow<PersonalizationPrefs> =
        database.aetherQueries.selectAllPreferences()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.associate { it.prefKey to it.prefValue }.toPrefs() }

    suspend fun savePrefs(prefs: PersonalizationPrefs) {
        val q = database.aetherQueries
        q.transaction {
            q.upsertPreference(KEY_PERSONA, prefs.persona.name)
            q.upsertPreference(KEY_TONE, prefs.tone.name)
            q.upsertPreference(KEY_PLANNING, prefs.planningStyle.name)
            q.upsertPreference(KEY_CHRONOTYPE, prefs.selfChronotype?.name ?: AUTO)
            q.upsertPreference(KEY_WAKE, prefs.wakeHour.toString())
            q.upsertPreference(KEY_SLEEP, prefs.sleepHour.toString())
            q.upsertPreference(KEY_MODULES, prefs.enabledModules.sorted().joinToString(","))
            q.upsertPreference(KEY_LEARNING, prefs.learningEnabled.toString())
            q.upsertPreference(KEY_ONBOARDED, prefs.onboarded.toString())
        }
    }

    fun observeBehaviorEvents(limit: Long = 500): Flow<List<BehaviorEvent>> =
        database.aetherQueries.selectRecentBehaviorEvents(limit)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.mapNotNull { row ->
                    val type = runCatching { BehaviorEventType.valueOf(row.type) }.getOrNull() ?: return@mapNotNull null
                    BehaviorEvent(row.id, row.createdAt, type, row.domain, row.hourOfDay.toInt())
                }
            }

    suspend fun recordEvent(type: BehaviorEventType, domain: String?, hourOfDay: Int) {
        database.aetherQueries.insertBehaviorEvent(
            id = generateId(),
            createdAt = currentTimeMillis(),
            type = type.name,
            domain = domain,
            hourOfDay = hourOfDay.toLong()
        )
    }

    /** "Forget what you learned about me" — clears implicit feedback only; explicit choices are untouched. */
    suspend fun resetLearning() {
        database.aetherQueries.deleteAllBehaviorEvents()
    }

    private fun Map<String, String>.toPrefs(): PersonalizationPrefs {
        val defaults = PersonalizationPrefs()
        return PersonalizationPrefs(
            persona = enumOrNull<Persona>(this[KEY_PERSONA]) ?: defaults.persona,
            tone = enumOrNull<CoachTone>(this[KEY_TONE]) ?: defaults.tone,
            planningStyle = enumOrNull<PlanningStyle>(this[KEY_PLANNING]) ?: defaults.planningStyle,
            selfChronotype = enumOrNull<Chronotype>(this[KEY_CHRONOTYPE]),
            wakeHour = this[KEY_WAKE]?.toIntOrNull()?.coerceIn(0, 23) ?: defaults.wakeHour,
            sleepHour = this[KEY_SLEEP]?.toIntOrNull()?.coerceIn(0, 23) ?: defaults.sleepHour,
            enabledModules = this[KEY_MODULES]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: defaults.enabledModules,
            learningEnabled = this[KEY_LEARNING]?.toBooleanStrictOrNull() ?: defaults.learningEnabled,
            onboarded = this[KEY_ONBOARDED]?.toBooleanStrictOrNull() ?: defaults.onboarded
        )
    }

    private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }

    private companion object {
        const val AUTO = "AUTO"
        const val KEY_PERSONA = "persona"
        const val KEY_TONE = "tone"
        const val KEY_PLANNING = "planningStyle"
        const val KEY_CHRONOTYPE = "selfChronotype"
        const val KEY_WAKE = "wakeHour"
        const val KEY_SLEEP = "sleepHour"
        const val KEY_MODULES = "enabledModules"
        const val KEY_LEARNING = "learningEnabled"
        const val KEY_ONBOARDED = "onboarded"
    }
}
