package com.aether.android.ui.gym

/**
 * Push / Pull / Legs / Full Body — a standard 4-day split. No photos/GIFs
 * yet, same reasoning as before: can't reliably source or verify an image
 * pipeline from inside this environment, so these are accurate text posture
 * cues instead of guessing at an image integration that might silently break.
 */
data class Exercise(val name: String, val postureCue: String)
data class WorkoutDay(val title: String, val exercises: List<Exercise>)

val FOUR_DAY_SPLIT = listOf(
    WorkoutDay(
        "Day 1 · Push (Chest, Shoulders, Triceps)",
        listOf(
            Exercise("Barbell Bench Press", "Shoulder blades pinched back, feet flat, bar to mid-chest, elbows ~45° from torso."),
            Exercise("Overhead Press", "Brace core, bar path stays close to face, don't overarch the lower back."),
            Exercise("Incline Dumbbell Press", "30-45° incline, control the negative, don't let elbows flare past shoulder line."),
            Exercise("Lateral Raise", "Slight elbow bend, lead with elbows not hands, stop at shoulder height."),
            Exercise("Cable Triceps Pushdown", "Elbows pinned to sides, full extension, no swinging.")
        )
    ),
    WorkoutDay(
        "Day 2 · Pull (Back, Biceps)",
        listOf(
            Exercise("Deadlift", "Neutral spine, bar close to shins, drive through heels, hips and shoulders rise together."),
            Exercise("Lat Pulldown / Pull-ups", "Lead with elbows, squeeze shoulder blades at the bottom, avoid swinging."),
            Exercise("Barbell Row", "Flat back, hinge at hips, pull to lower ribcage, no jerking the weight up."),
            Exercise("Face Pull", "Pull to eye level, externally rotate at the end, focus on rear delts."),
            Exercise("Barbell Bicep Curl", "Elbows fixed at sides, no swinging the torso, full range of motion.")
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
        "Day 4 · Full Body",
        listOf(
            Exercise("Front Squat", "Elbows high, bar rests on front delts, torso stays upright through the descent."),
            Exercise("Weighted Pull-ups", "Full hang at the bottom, chest to bar, no kipping."),
            Exercise("Dumbbell Overhead Press", "Neutral or slight forward grip, ribs down, don't hyperextend the lower back."),
            Exercise("Hip Thrust", "Chin tucked, drive through heels, full lockout squeeze at the top."),
            Exercise("Plank", "Straight line from shoulders to heels, don't let hips sag or pike.")
        )
    )
)
