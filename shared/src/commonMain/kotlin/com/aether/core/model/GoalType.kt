package com.aether.core.model

/**
 * The rung a Goal occupies in the life hierarchy:
 * Life Vision -> Long Term -> Domain -> Quarterly -> Monthly -> Weekly -> (Task).
 * `Goal.domain` stays the category label (Academic/Career/Health/.../Gym/GATE/...)
 * for DOMAIN-level goals and below — orthogonal to this rung.
 */
enum class GoalType {
    LIFE_VISION,
    LONG_TERM,
    DOMAIN,
    QUARTERLY,
    MONTHLY,
    WEEKLY
}

/** The natural next rung down, used to default a new goal's type to "one level under its parent." */
fun GoalType?.nextChildType(): GoalType = when (this) {
    null -> GoalType.LIFE_VISION
    GoalType.LIFE_VISION -> GoalType.LONG_TERM
    GoalType.LONG_TERM -> GoalType.DOMAIN
    GoalType.DOMAIN -> GoalType.QUARTERLY
    GoalType.QUARTERLY -> GoalType.MONTHLY
    GoalType.MONTHLY, GoalType.WEEKLY -> GoalType.WEEKLY
}
