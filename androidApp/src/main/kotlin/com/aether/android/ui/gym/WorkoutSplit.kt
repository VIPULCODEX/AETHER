package com.aether.android.ui.gym

/**
 * Curated, text-described 4-day split. No photos/GIFs are included yet —
 * that needs either bundled licensed images or a live exercise-database
 * API, neither of which can be safely wired up without the ability to
 * verify them here. The posture cues below are the well-established,
 * standard form points for each lift.
 */
data class Exercise(val name: String, val postureCue: String)
data class WorkoutDay(val title: String, val exercises: List<Exercise>)

val FOUR_DAY_SPLIT = listOf(
    WorkoutDay(
        "Day 1 · Chest & Triceps",
        listOf(
            Exercise("Barbell Bench Press", "Shoulder blades pinched back, feet flat, bar to mid-chest, elbows ~45° from torso."),
            Exercise("Incline Dumbbell Press", "30-45° incline, control the negative, don't let elbows flare past shoulder line."),
            Exercise("Chest Dips", "Lean forward slightly for chest emphasis, stop at 90° elbow bend, don't overextend shoulders."),
            Exercise("Cable Triceps Pushdown", "Elbows pinned to sides, full extension, no swinging."),
            Exercise("Overhead Triceps Extension", "Elbows stay close to head, full stretch at the bottom.")
        )
    ),
    WorkoutDay(
        "Day 2 · Back & Biceps",
        listOf(
            Exercise("Deadlift", "Neutral spine, bar close to shins, drive through heels, hips and shoulders rise together."),
            Exercise("Lat Pulldown / Pull-ups", "Lead with elbows, squeeze shoulder blades at the bottom, avoid swinging."),
            Exercise("Barbell Row", "Flat back, hinge at hips, pull to lower ribcage, no jerking the weight up."),
            Exercise("Barbell Bicep Curl", "Elbows fixed at sides, no swinging the torso, full range of motion."),
            Exercise("Hammer Curl", "Neutral grip, controlled tempo, elbows stay stationary.")
        )
    ),
    WorkoutDay(
        "Day 3 · Legs",
        listOf(
            Exercise("Barbell Back Squat", "Bar on upper traps, chest up, knees track over toes, hit at least parallel depth."),
            Exercise("Romanian Deadlift", "Soft knee bend, hinge at hips, bar stays close to legs, feel the hamstring stretch."),
            Exercise("Leg Press", "Feet shoulder-width, don't lock knees at top, control the descent."),
            Exercise("Leg Curl", "Slow controlled tempo, avoid hips lifting off the pad."),
            Exercise("Standing Calf Raise", "Full stretch at the bottom, pause at the top, controlled tempo.")
        )
    ),
    WorkoutDay(
        "Day 4 · Shoulders & Abs",
        listOf(
            Exercise("Overhead Press", "Brace core, bar path stays close to face, don't overarch the lower back."),
            Exercise("Lateral Raise", "Slight elbow bend, lead with elbows not hands, stop at shoulder height."),
            Exercise("Face Pull", "Pull to eye level, externally rotate at the end, focus on rear delts."),
            Exercise("Hanging Leg Raise", "Avoid swinging, control the descent, engage the lower abs."),
            Exercise("Plank", "Straight line from shoulders to heels, don't let hips sag or pike.")
        )
    )
)
