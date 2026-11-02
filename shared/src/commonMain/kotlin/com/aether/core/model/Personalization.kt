package com.aether.core.model

/**
 * Starting point for someone's needs, not a box they're stuck in: picking a
 * persona seeds sensible defaults (modules, tone, which domains get a boost),
 * and every one of those stays individually overridable afterwards. What the
 * app *learns* from behavior then layers on top — see PersonalizationEngine.
 */
enum class Persona(val label: String, val blurb: String, val boostedDomains: Set<String>, val modules: Set<String>) {
    STUDENT("Student", "Exams, coursework, steady revision.", setOf("Academic", "GATE", "JEE", "College Work"), setOf(Modules.JOURNAL, Modules.RESEARCH, Modules.GYM)),
    RESEARCHER("Researcher", "Papers, experiments, deep-work blocks.", setOf("Research", "Academic"), setOf(Modules.JOURNAL, Modules.RESEARCH)),
    ATHLETE("Athlete", "Training, recovery, body progress.", setOf("Gym", "Health"), setOf(Modules.JOURNAL, Modules.GYM)),
    BUILDER("Professional", "Career, projects, money goals.", setOf("Career", "Financial"), setOf(Modules.JOURNAL, Modules.RESEARCH)),
    CUSTOM("Custom", "Start from scratch and choose everything.", emptySet(), Modules.ALL)
}

object Modules {
    const val GYM = "gym"
    const val RESEARCH = "research"
    const val JOURNAL = "journal"
    val ALL = setOf(GYM, RESEARCH, JOURNAL)

    /** Which module a goal/task domain belongs to, or null if it isn't tied to a toggleable module. */
    fun forDomain(domain: String): String? = when (domain.lowercase()) {
        "gym", "health" -> GYM
        "research" -> RESEARCH
        else -> null
    }
}

enum class CoachTone(val label: String) {
    GENTLE("Gentle"),
    DIRECT("Direct"),
    DATA("Just the data")
}

enum class PlanningStyle(val label: String) {
    STRUCTURED("Structured"),
    FLEXIBLE("Flexible")
}

enum class Chronotype(val label: String, val peakStartHour: Int) {
    MORNING("Morning", 8),
    DAY("Daytime", 13),
    EVENING("Evening", 18),
    NIGHT("Night", 22)
}

/**
 * Everything the user states explicitly. Learned behavior never overwrites
 * these: [selfChronotype] == null means "figure it out from my behavior",
 * a non-null value means "trust me over the data".
 */
data class PersonalizationPrefs(
    val persona: Persona = Persona.CUSTOM,
    val tone: CoachTone = CoachTone.DIRECT,
    val planningStyle: PlanningStyle = PlanningStyle.FLEXIBLE,
    val selfChronotype: Chronotype? = null,
    val wakeHour: Int = 7,
    val sleepHour: Int = 23,
    val enabledModules: Set<String> = Modules.ALL,
    val learningEnabled: Boolean = true,
    val onboarded: Boolean = false
) {
    fun withPersona(persona: Persona): PersonalizationPrefs = copy(
        persona = persona,
        enabledModules = persona.modules,
        tone = when (persona) {
            Persona.RESEARCHER -> CoachTone.DATA
            Persona.STUDENT -> CoachTone.GENTLE
            else -> tone
        },
        planningStyle = when (persona) {
            Persona.STUDENT, Persona.ATHLETE -> PlanningStyle.STRUCTURED
            Persona.RESEARCHER, Persona.BUILDER -> PlanningStyle.FLEXIBLE
            Persona.CUSTOM -> planningStyle
        },
        onboarded = true
    )

    /** True if [hour] falls inside the user's declared waking hours (handles wake > sleep wraparound, e.g. night owls). */
    fun isAwake(hour: Int): Boolean =
        if (wakeHour <= sleepHour) hour in wakeHour until sleepHour else hour >= wakeHour || hour < sleepHour
}

enum class BehaviorEventType { SUGGESTION_ACCEPTED, SUGGESTION_DISMISSED }

data class BehaviorEvent(
    val id: String,
    val createdAt: Long,
    val type: BehaviorEventType,
    val domain: String?,
    val hourOfDay: Int
)
