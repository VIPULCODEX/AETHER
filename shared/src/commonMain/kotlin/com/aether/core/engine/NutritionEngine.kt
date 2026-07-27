package com.aether.core.engine

import com.aether.core.model.UserProfile
import kotlin.math.roundToInt

enum class ActivityLevel(val multiplier: Double, val label: String) {
    SEDENTARY(1.2, "Sedentary (little/no exercise)"),
    LIGHT(1.375, "Light (1-3 days/week)"),
    MODERATE(1.55, "Moderate (3-5 days/week)"),
    ACTIVE(1.725, "Active (6-7 days/week)"),
    VERY_ACTIVE(1.9, "Very active (physical job + training)")
}

enum class BodyGoal(val label: String) {
    CUT("Lose fat"),
    MAINTAIN("Maintain"),
    BULK("Build muscle")
}

data class NutritionPlan(
    val bmr: Int,
    val tdee: Int,
    val targetCalories: Int,
    val proteinGrams: Int
)

/**
 * Standard, well-established formulas (Mifflin-St Jeor for BMR, activity
 * multipliers for TDEE) — general fitness guidance, not medical advice.
 */
class NutritionEngine {

    fun compute(profile: UserProfile, activityLevel: ActivityLevel, goal: BodyGoal): NutritionPlan? {
        val height = profile.heightCm ?: return null
        val weight = profile.weightKg ?: return null
        val age = profile.age ?: return null
        val isMale = profile.isMale ?: return null

        val bmr = if (isMale) {
            10 * weight + 6.25 * height - 5 * age + 5
        } else {
            10 * weight + 6.25 * height - 5 * age - 161
        }

        val tdee = bmr * activityLevel.multiplier

        val targetCalories = when (goal) {
            BodyGoal.CUT -> tdee - 500
            BodyGoal.MAINTAIN -> tdee
            BodyGoal.BULK -> tdee + 400
        }

        // Protein per kg bodyweight: higher during a cut to preserve muscle,
        // moderate on a bulk, baseline at maintenance.
        val proteinPerKg = when (goal) {
            BodyGoal.CUT -> 2.0
            BodyGoal.MAINTAIN -> 1.6
            BodyGoal.BULK -> 1.8
        }

        return NutritionPlan(
            bmr = bmr.roundToInt(),
            tdee = tdee.roundToInt(),
            targetCalories = targetCalories.roundToInt(),
            proteinGrams = (weight * proteinPerKg).roundToInt()
        )
    }
}
