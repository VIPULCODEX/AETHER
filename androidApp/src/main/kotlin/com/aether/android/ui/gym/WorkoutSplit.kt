package com.aether.android.ui.gym

/**
 * Push / Pull / Legs / Full Body — a standard 4-day split. No photos/GIFs
 * yet, same reasoning as before: can't reliably source or verify an image
 * pipeline from inside this environment, so these are accurate text posture
 * cues instead of guessing at an image integration that might silently break.
 */
data class Exercise(val name: String, val setsReps: String, val postureCue: String)
data class WorkoutDay(val title: String, val subtitle: String, val exercises: List<Exercise>)

val FOUR_DAY_SPLIT = listOf(
    WorkoutDay(
        "Day 1 · Push",
        "Chest, Shoulders, Triceps",
        listOf(
            Exercise("Barbell Bench Press", "4 × 5-6", "Shoulder blades pinched back, feet flat, bar to mid-chest, elbows ~45° from torso."),
            Exercise("Overhead Press", "3 × 6-8", "Brace core, bar path stays close to face, don't overarch the lower back."),
            Exercise("Incline Dumbbell Press", "3 × 8-10", "30-45° incline, control the negative, don't let elbows flare past shoulder line."),
            Exercise("Lateral Raise", "3 × 12-15", "Slight elbow bend, lead with elbows not hands, stop at shoulder height."),
            Exercise("Cable Triceps Pushdown", "3 × 10-12", "Elbows pinned to sides, full extension, no swinging.")
        )
    ),
    WorkoutDay(
        "Day 2 · Pull",
        "Back, Biceps",
        listOf(
            Exercise("Deadlift", "3 × 4-6", "Neutral spine, bar close to shins, drive through heels, hips and shoulders rise together."),
            Exercise("Lat Pulldown / Pull-ups", "4 × 6-10", "Lead with elbows, squeeze shoulder blades at the bottom, avoid swinging."),
            Exercise("Barbell Row", "3 × 8-10", "Flat back, hinge at hips, pull to lower ribcage, no jerking the weight up."),
            Exercise("Face Pull", "3 × 12-15", "Pull to eye level, externally rotate at the end, focus on rear delts."),
            Exercise("Barbell Bicep Curl", "3 × 10-12", "Elbows fixed at sides, no swinging the torso, full range of motion.")
        )
    ),
    WorkoutDay(
        "Day 3 · Legs",
        "Quads, Hamstrings, Calves",
        listOf(
            Exercise("Barbell Back Squat", "4 × 5-6", "Bar on upper traps, chest up, knees track over toes, hit at least parallel depth."),
            Exercise("Romanian Deadlift", "3 × 8-10", "Soft knee bend, hinge at hips, bar stays close to legs, feel the hamstring stretch."),
            Exercise("Leg Press", "3 × 10-12", "Feet shoulder-width, don't lock knees at top, control the descent."),
            Exercise("Leg Curl", "3 × 10-12", "Slow controlled tempo, avoid hips lifting off the pad."),
            Exercise("Standing Calf Raise", "4 × 12-15", "Full stretch at the bottom, pause at the top, controlled tempo.")
        )
    ),
    WorkoutDay(
        "Day 4 · Full Body",
        "Compound priority, lighter volume",
        listOf(
            Exercise("Front Squat", "3 × 6-8", "Elbows high, bar rests on front delts, torso stays upright through the descent."),
            Exercise("Weighted Pull-ups", "3 × 6-8", "Full hang at the bottom, chest to bar, no kipping."),
            Exercise("Dumbbell Overhead Press", "3 × 8-10", "Neutral or slight forward grip, ribs down, don't hyperextend the lower back."),
            Exercise("Hip Thrust", "3 × 8-10", "Chin tucked, drive through heels, full lockout squeeze at the top."),
            Exercise("Plank", "3 × 40-60s", "Straight line from shoulders to heels, don't let hips sag or pike.")
        )
    )
)
