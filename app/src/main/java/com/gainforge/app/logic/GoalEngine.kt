package com.gainforge.app.logic

import com.gainforge.app.data.*
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

enum class GoalMode { BULK, RECOMP, MAINTAIN }

data class Plan(
    val bmi: Double,
    val bodyFat: Double,
    val bodyFatEstimated: Boolean,
    val leanKg: Double,
    val ffmi: Double,
    val targetFfmi: Double,
    val targetBodyFat: Double,
    val systemTargetKg: Double,
    val targetKg: Double,
    val mode: GoalMode,
    val weeklyGainKg: Double,
    val weeks: Int,
    val eta: LocalDate,
    val bmr: Int,
    val tdee: Int,
    val surplus: Int,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val waterMl: Int,
    val warnings: List<String>,
    val notes: List<String>
)

/**
 * The "system" that sets an ideal goal.
 *
 * 1. Estimates body fat (your number, or the Deurenberg BMI formula).
 * 2. Works out lean mass and FFMI (fat-free mass index = lean kg / height m²).
 * 3. Each physique has a target FFMI + body-fat %. Ideal weight = target lean mass / (1 − target BF).
 * 4. Gain rate depends on training age (beginners grow fastest) and the pace you pick.
 * 5. Calories = Mifflin-St Jeor BMR × daily activity + training burn + surplus for that gain rate.
 */
object GoalEngine {

    fun estimateBodyFat(h: Health): Double {
        val m = h.heightCm / 100.0
        val bmi = h.weightKg / (m * m)
        val male = if (h.sex == Sex.MALE) 1 else 0
        return (1.2 * bmi + 0.23 * h.age - 10.8 * male - 5.4).coerceIn(5.0, 45.0)
    }

    fun compute(p: Profile): Plan {
        val h = p.health
        val m = h.heightCm / 100.0
        val w = h.weightKg
        val male = h.sex == Sex.MALE
        val bmi = w / (m * m)

        val bfEstimated = h.bodyFat == null
        val bf = (h.bodyFat ?: estimateBodyFat(h)).coerceIn(4.0, 50.0)
        val lean = w * (1 - bf / 100)
        val ffmi = lean / (m * m)

        val ph = p.goal.physique
        val tFfmi = if (male) ph.ffmiMale else ph.ffmiFemale
        val tBf = if (male) ph.bfMale else ph.bfFemale
        val tLean = max(tFfmi * m * m, lean * 1.03)
        val systemTarget = tLean / (1 - tBf / 100)

        val custom = p.goal.customTargetKg
        val mode = when {
            custom != null && custom <= w + 0.3 -> GoalMode.MAINTAIN
            custom == null && systemTarget <= w + 0.5 -> GoalMode.RECOMP
            else -> GoalMode.BULK
        }
        val target = custom ?: if (mode == GoalMode.RECOMP) w else systemTarget

        val monthlyPct = when (h.experience) {
            Experience.BEGINNER -> 1.25
            Experience.INTERMEDIATE -> 0.75
            Experience.ADVANCED -> 0.4
        } * p.goal.pace.mult * (if (bmi < 18.5) 1.2 else 1.0) * (if (h.age >= 40) 0.85 else 1.0)
        val weekly = w * monthlyPct / 100 / 4.345

        val surplus = when (mode) {
            GoalMode.BULK -> (weekly * 1100).roundToInt().coerceIn(150, 700)
            GoalMode.RECOMP -> 100
            GoalMode.MAINTAIN -> 0
        }

        val bmr = 10 * w + 6.25 * h.heightCm - 5 * h.age + if (male) 5 else -161
        val trainingKcal = h.days * h.sessionMin * 5.5 / 7.0
        val tdee = bmr * h.activity.factor + trainingKcal
        val calories = ((tdee + surplus) / 10).roundToInt() * 10

        val protein = (if (bf > 28) lean * 2.6 else w * ph.proteinPerKg).roundToInt()
        val fat = max(0.8 * w, calories * 0.25 / 9).roundToInt()
        val carbs = ((calories - protein * 4 - fat * 9) / 4.0).roundToInt().coerceAtLeast(100)
        val water = ((0.035 * w + 0.5 + h.days * h.sessionMin / 60.0 / 7 * 0.5) * 1000 / 50).roundToInt() * 50

        val weeks = when (mode) {
            GoalMode.BULK -> ceil((target - w) / weekly).toInt().coerceAtLeast(1)
            GoalMode.RECOMP -> ((tLean - lean) / (weekly * 0.5)).roundToInt().coerceIn(8, 260)
            GoalMode.MAINTAIN -> 0
        }

        val warnings = buildList {
            if (bmi < 17.5) add("Your BMI is low (%.1f). Please check with a doctor before starting — rule out medical reasons for low weight.".format(bmi))
            if (h.age < 18) add("You're under 18. Focus on technique and eating well; get a parent or coach involved, and skip max-effort lifts.")
            if (Condition.HEART in h.conditions) add("Heart condition: get clearance from your doctor before training, and stop if you feel chest pain, dizziness or unusual breathlessness.")
            if (Condition.HIGH_BP in h.conditions) add("High blood pressure: don't hold your breath on lifts; keep most sets 2–3 reps short of failure. Heavy singles have been removed from your plan.")
            if (Condition.DIABETES in h.conditions) add("Diabetes: spread carbs across meals and check glucose around workouts. Your doctor or dietitian should approve the calorie increase.")
        }
        val notes = buildList {
            if (Condition.KNEE in h.conditions) add("Knee-friendly swaps are in your program (box squats, step-ups, glute bridges).")
            if (Condition.LOWER_BACK in h.conditions) add("Back-friendly swaps are in your program (hip thrusts, chest-supported rows).")
            if (Condition.SHOULDER in h.conditions) add("Shoulder-friendly swaps are in your program (neutral-grip and landmine presses).")
            if (Condition.ASTHMA in h.conditions) add("Asthma: warm up for 10 minutes and keep your inhaler nearby.")
            if (h.sleepHours < 7) add("You sleep about ${h.sleepHours.roundToInt()} h. Aim for 7–9 h — muscle is built while you recover.")
            if (mode == GoalMode.BULK) add("Weigh yourself 3 mornings a week. If your weekly average isn't going up after 2 weeks, add 150 kcal a day.")
            if (mode == GoalMode.RECOMP) add("You already carry enough weight for this look, so the plan builds muscle at about your current weight while body fat slowly drops.")
            if (p.food.appetite == Appetite.SMALL) add("Small appetite: shakes, nut butters, olive oil and whole milk add calories without filling you up.")
            add("Eat ${(protein / max(3, p.food.mealsPerDay).toDouble()).roundToInt()} g+ of protein at each of your ${p.food.mealsPerDay} meals.")
        }

        return Plan(
            bmi = bmi, bodyFat = bf, bodyFatEstimated = bfEstimated, leanKg = lean, ffmi = ffmi,
            targetFfmi = tFfmi, targetBodyFat = tBf, systemTargetKg = systemTarget, targetKg = target,
            mode = mode, weeklyGainKg = weekly, weeks = weeks, eta = LocalDate.now().plusWeeks(weeks.toLong()),
            bmr = bmr.roundToInt(), tdee = tdee.roundToInt(), surplus = surplus, calories = calories,
            protein = protein, carbs = carbs, fat = fat, waterMl = water, warnings = warnings, notes = notes
        )
    }
}
