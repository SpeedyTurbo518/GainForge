package com.gainforge.app.logic

import com.gainforge.app.data.*
import com.gainforge.app.data.Condition.*

data class Slot(
    val muscle: String,
    val gym: String,
    val dumbbell: String,
    val bodyweight: String,
    val compound: Boolean,
    val avoid: Set<Condition> = emptySet(),
    val safeAlt: String = "",
    val fixedReps: String? = null
)

data class PlannedExercise(val name: String, val muscle: String, val sets: Int, val reps: String, val restSec: Int, val note: String? = null)
data class WorkoutDay(val title: String, val focus: String, val exercises: List<PlannedExercise>, val finisher: String?) {
    val totalSets get() = exercises.sumOf { it.sets }
    val estMinutes get() = (exercises.sumOf { it.sets * (45 + it.restSec) } / 60.0).toInt() + 8
}
data class Program(val name: String, val days: List<WorkoutDay>)

private data class Scheme(val cSets: Int, val cReps: String, val cRest: Int, val iSets: Int, val iReps: String, val iRest: Int, val finisher: String?)

object Programs {
    private val lib = mapOf(
        "squat" to Slot("Quads", "Barbell Back Squat", "Goblet Squat", "Tempo Bodyweight Squat", true, setOf(KNEE), "Box Squat to a High Box"),
        "rdl" to Slot("Hamstrings", "Romanian Deadlift", "Dumbbell Romanian Deadlift", "Single-Leg Hip Hinge", true, setOf(LOWER_BACK), "Hip Thrust"),
        "legpress" to Slot("Quads", "Leg Press", "Dumbbell Bulgarian Split Squat", "Bulgarian Split Squat", true, setOf(KNEE), "Glute Bridge"),
        "lunge" to Slot("Glutes", "Dumbbell Walking Lunge", "Dumbbell Reverse Lunge", "Reverse Lunge", false, setOf(KNEE), "Low Box Step-Up"),
        "legcurl" to Slot("Hamstrings", "Lying Leg Curl", "Dumbbell Hamstring Curl", "Sliding Leg Curl", false),
        "calf" to Slot("Calves", "Standing Calf Raise", "Dumbbell Calf Raise", "Single-Leg Calf Raise", false),
        "hipthrust" to Slot("Glutes", "Barbell Hip Thrust", "Dumbbell Hip Thrust", "Single-Leg Glute Bridge", true),
        "bench" to Slot("Chest", "Barbell Bench Press", "Dumbbell Bench Press", "Push-Ups", true, setOf(SHOULDER), "Neutral-Grip Dumbbell Floor Press"),
        "incline" to Slot("Upper chest", "Incline Dumbbell Press", "Incline Dumbbell Press", "Feet-Elevated Push-Ups", true, setOf(SHOULDER), "Low-Incline Neutral-Grip Press"),
        "fly" to Slot("Chest", "Cable Fly", "Dumbbell Fly", "Wide Push-Ups", false),
        "dips" to Slot("Triceps", "Weighted Dips", "Dumbbell Close-Grip Press", "Bench Dips", false, setOf(SHOULDER), "Cable Triceps Pushdown"),
        "ohp" to Slot("Shoulders", "Standing Overhead Press", "Seated Dumbbell Shoulder Press", "Pike Push-Ups", true, setOf(SHOULDER), "Half-Kneeling Landmine Press"),
        "lateral" to Slot("Side delts", "Cable Lateral Raise", "Dumbbell Lateral Raise", "Band Lateral Raise", false),
        "reardelt" to Slot("Rear delts", "Face Pull", "Dumbbell Rear-Delt Fly", "Prone Y-T-W Raise", false),
        "row" to Slot("Back", "Barbell Row", "One-Arm Dumbbell Row", "Inverted Table Row", true, setOf(LOWER_BACK), "Chest-Supported Row"),
        "pulldown" to Slot("Lats", "Lat Pulldown", "Dumbbell Pullover", "Towel Door Row", true),
        "pullup" to Slot("Lats", "Weighted Pull-Ups", "Chest-Supported Dumbbell Row", "Pull-Ups (or Negatives)", true),
        "curl" to Slot("Biceps", "EZ-Bar Curl", "Dumbbell Curl", "Towel Curl", false),
        "hammer" to Slot("Biceps", "Rope Hammer Curl", "Dumbbell Hammer Curl", "Chin-Up Hold", false),
        "triceps" to Slot("Triceps", "Cable Pushdown", "Dumbbell Overhead Extension", "Diamond Push-Ups", false),
        "skull" to Slot("Triceps", "EZ-Bar Skull Crusher", "Dumbbell Skull Crusher", "Bench Dips", false),
        "abs" to Slot("Core", "Cable Crunch", "Weighted Crunch", "Hollow Body Hold", false),
        "plank" to Slot("Core", "Plank", "Plank", "Plank", false, fixedReps = "30–45 s")
    )

    private val fbA = Triple("Full Body A", "Squat · Press · Row", listOf("squat", "bench", "row", "ohp", "curl", "abs"))
    private val fbB = Triple("Full Body B", "Hinge · Incline · Pull", listOf("rdl", "incline", "pulldown", "lunge", "lateral", "triceps"))
    private val fbC = Triple("Full Body C", "Glutes · Chest · Back", listOf("hipthrust", "bench", "pullup", "legcurl", "reardelt", "hammer"))
    private val upA = Triple("Upper A", "Chest · Back · Arms", listOf("bench", "row", "ohp", "pulldown", "curl", "triceps"))
    private val loA = Triple("Lower A", "Quads · Hamstrings", listOf("squat", "rdl", "legcurl", "calf", "abs"))
    private val upB = Triple("Upper B", "Incline · Pull-Ups · Delts", listOf("incline", "pullup", "lateral", "fly", "hammer", "skull"))
    private val loB = Triple("Lower B", "Glutes · Single-Leg", listOf("legpress", "hipthrust", "lunge", "calf", "plank"))
    private val push = Triple("Push", "Chest · Shoulders · Triceps", listOf("bench", "ohp", "incline", "lateral", "triceps", "dips"))
    private val pull = Triple("Pull", "Back · Rear Delts · Biceps", listOf("pullup", "row", "pulldown", "reardelt", "curl", "hammer"))
    private val legs = Triple("Legs", "Quads · Hamstrings · Calves", listOf("squat", "rdl", "legpress", "legcurl", "calf", "abs"))
    private val push2 = Triple("Push 2", "Upper Chest · Delts", listOf("incline", "ohp", "fly", "lateral", "skull", "dips"))
    private val pull2 = Triple("Pull 2", "Width · Thickness", listOf("pulldown", "row", "pullup", "reardelt", "hammer", "curl"))
    private val legs2 = Triple("Legs 2", "Glutes · Single-Leg", listOf("legpress", "hipthrust", "lunge", "legcurl", "calf", "plank"))

    private fun scheme(ph: Physique) = when (ph) {
        Physique.LEAN -> Scheme(3, "8–12", 75, 3, "12–15", 45, "Finisher: 10 min incline walk or easy bike")
        Physique.ATHLETIC -> Scheme(4, "6–10", 120, 3, "10–12", 60, "Finisher: 5 × 15 s hill or bike sprints")
        Physique.BODYBUILDER -> Scheme(4, "8–12", 120, 4, "10–15", 60, "Pump finisher: one drop set on your last exercise")
        Physique.POWER -> Scheme(5, "3–5", 180, 3, "8–10", 90, null)
    }

    fun build(p: Profile): Program {
        val h = p.health
        val days = h.days.coerceIn(2, 6)
        val templates = when (days) {
            2 -> listOf(fbA, fbB)
            3 -> listOf(fbA, fbB, fbC)
            4 -> listOf(upA, loA, upB, loB)
            5 -> listOf(push, pull, legs, upB, loB)
            else -> listOf(push, pull, legs, push2, pull2, legs2)
        }
        val splitName = when (days) {
            2, 3 -> "Full Body"
            4 -> "Upper / Lower"
            5 -> "Push · Pull · Legs + Upper/Lower"
            else -> "Push · Pull · Legs ×2"
        }
        val s = scheme(p.goal.physique)
        val cautiousHeart = HIGH_BP in h.conditions || HEART in h.conditions
        val beginner = h.experience == Experience.BEGINNER
        val maxExercises = when {
            h.sessionMin <= 30 -> 4
            h.sessionMin <= 45 -> 5
            else -> 6
        }

        val dayList = templates.map { (title, focus, keys) ->
            val exercises = keys.take(maxExercises).map { key ->
                val slot = lib.getValue(key)
                val blocked = slot.avoid.firstOrNull { it in h.conditions }
                val name = when {
                    blocked != null -> slot.safeAlt
                    h.equipment == Equipment.GYM -> slot.gym
                    h.equipment == Equipment.DUMBBELLS -> slot.dumbbell
                    else -> slot.bodyweight
                }
                var sets = if (slot.compound) s.cSets else s.iSets
                if (beginner) sets = (sets - 1).coerceAtLeast(2)
                var reps = if (slot.compound) s.cReps else s.iReps
                if (cautiousHeart && slot.compound && p.goal.physique == Physique.POWER) reps = "6–8"
                if (h.equipment == Equipment.BODYWEIGHT) reps = if (slot.compound) "10–20" else "15–20"
                reps = slot.fixedReps ?: reps
                val rest = if (slot.compound) s.cRest else s.iRest
                PlannedExercise(
                    name = name, muscle = slot.muscle, sets = sets, reps = reps, restSec = rest,
                    note = blocked?.let { "Swapped for your ${it.label.lowercase()}" }
                )
            }
            WorkoutDay(title, focus, exercises, s.finisher)
        }
        return Program("$days-day $splitName · ${p.goal.physique.title}", dayList)
    }

    fun today(p: Profile): WorkoutDay {
        val prog = build(p)
        return prog.days[p.programDay.mod(prog.days.size)]
    }
}
