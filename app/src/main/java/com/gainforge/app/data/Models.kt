package com.gainforge.app.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

@Serializable
enum class Sex(val label: String) { MALE("Male"), FEMALE("Female") }

/** Daily movement outside of planned workouts (training calories are added separately). */
@Serializable
enum class Activity(val label: String, val detail: String, val factor: Double) {
    SEDENTARY("Mostly sitting", "Desk job, little walking", 1.2),
    LIGHT("Lightly active", "On your feet part of the day", 1.32),
    MODERATE("Active", "Walk a lot or have a physical job", 1.45),
    VERY("Very active", "Manual labor or sports most days", 1.6)
}

@Serializable
enum class Experience(val label: String, val detail: String) {
    BEGINNER("Beginner", "Less than 1 year of lifting"),
    INTERMEDIATE("Intermediate", "1–3 years of steady training"),
    ADVANCED("Advanced", "3+ years, consistent and strong")
}

@Serializable
enum class Equipment(val label: String, val detail: String) {
    GYM("Full gym", "Barbells, cables and machines"),
    DUMBBELLS("Home dumbbells", "Dumbbells and a bench"),
    BODYWEIGHT("No equipment", "Bodyweight only")
}

@Serializable
enum class Condition(val label: String) {
    KNEE("Knee pain / injury"),
    LOWER_BACK("Lower back issues"),
    SHOULDER("Shoulder injury"),
    HIGH_BP("High blood pressure"),
    HEART("Heart condition"),
    DIABETES("Diabetes"),
    ASTHMA("Asthma")
}

@Serializable
enum class Physique(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val detail: String,
    val ffmiMale: Double,
    val ffmiFemale: Double,
    val bfMale: Double,
    val bfFemale: Double,
    val proteinPerKg: Double
) {
    LEAN("Lean", "Defined & athletic-slim", "🏃", "Visible abs, toned arms and shoulders. Moderate size, low body fat.", 20.0, 16.5, 11.0, 20.0, 2.0),
    ATHLETIC("Athletic", "Muscle + performance", "⚡", "A fit, powerful look — strong, fast and well-proportioned.", 21.5, 17.5, 13.0, 22.0, 2.0),
    BODYBUILDER("Bodybuilder", "Maximum size & symmetry", "🏆", "Big, full muscles with a classic V-taper. Highest training volume.", 23.5, 19.0, 12.0, 20.0, 2.2),
    POWER("Powerbuilder", "Big, strong & dense", "🏋️", "Heavy lifts and thick muscle. Strength first, a bit more body fat.", 23.0, 18.5, 17.0, 25.0, 1.8)
}

@Serializable
enum class Pace(val label: String, val detail: String, val mult: Double) {
    STEADY("Steady", "Least fat gain", 0.7),
    STANDARD("Standard", "Recommended", 1.0),
    FAST("Fast", "Faster size, more fat", 1.35)
}

@Serializable
enum class Diet(val label: String) {
    NONE("No restrictions"), VEGETARIAN("Vegetarian"), VEGAN("Vegan"), PESCATARIAN("Pescatarian"), HALAL("Halal")
}

@Serializable
enum class Allergen(val label: String) {
    DAIRY("Dairy"), GLUTEN("Gluten"), NUTS("Nuts"), EGGS("Eggs"), SHELLFISH("Shellfish"), FISH("Fish"), SOY("Soy")
}

@Serializable
enum class Appetite(val label: String, val detail: String) {
    SMALL("Small appetite", "I get full fast — liquid calories help"),
    NORMAL("Normal", "I eat regular meals"),
    BIG("Big eater", "I can put food away")
}

@Serializable
enum class CookTime(val label: String, val detail: String, val maxMin: Int) {
    QUICK("Quick & easy", "15 minutes or less", 15),
    MODERATE("Some cooking", "Up to about 30 minutes", 30),
    CHEF("I love cooking", "Time isn't a problem", 999)
}

@Serializable
enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

@Serializable
data class Health(
    val sex: Sex = Sex.MALE,
    val age: Int = 25,
    val heightCm: Double = 175.0,
    val weightKg: Double = 68.0,
    val bodyFat: Double? = null,
    val activity: Activity = Activity.LIGHT,
    val experience: Experience = Experience.BEGINNER,
    val days: Int = 4,
    val sessionMin: Int = 60,
    val equipment: Equipment = Equipment.GYM,
    val conditions: Set<Condition> = emptySet(),
    val sleepHours: Double = 7.0,
    val restingHr: Int? = null
)

@Serializable
data class FoodPrefs(
    val diet: Diet = Diet.NONE,
    val allergies: Set<Allergen> = emptySet(),
    val proteins: Set<String> = emptySet(),
    val carbs: Set<String> = emptySet(),
    val cuisines: Set<String> = emptySet(),
    val dislikes: String = "",
    val appetite: Appetite = Appetite.NORMAL,
    val cookTime: CookTime = CookTime.MODERATE,
    val mealsPerDay: Int = 4
)

@Serializable
data class GoalSettings(
    val physique: Physique = Physique.ATHLETIC,
    val pace: Pace = Pace.STANDARD,
    val customTargetKg: Double? = null
)

@Serializable
data class WeightEntry(val date: String, val kg: Double)

@Serializable
data class WorkoutLog(val date: String, val title: String, val durationSec: Long, val sets: Int, val volumeKg: Double)

@Serializable
data class DayIntake(val kcal: Int = 0, val protein: Int = 0, val waterMl: Int = 0, val eaten: List<String> = emptyList())

@Serializable
data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val emoji: String = "💪",
    val photoPath: String? = null,
    val accent: Long = 0xFF0A84FF,
    val isFriend: Boolean = false,
    val health: Health = Health(),
    val food: FoodPrefs = FoodPrefs(),
    val goal: GoalSettings = GoalSettings(),
    val weights: List<WeightEntry> = emptyList(),
    val workouts: List<WorkoutLog> = emptyList(),
    val intake: Map<String, DayIntake> = emptyMap(),
    val favorites: Set<String> = emptySet(),
    val programDay: Int = 0,
    val planShuffle: Int = 0,
    val createdAt: String = LocalDate.now().toString()
)

@Serializable
data class AppData(
    val profiles: List<Profile> = emptyList(),
    val activeId: String? = null,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val imperial: Boolean = true
)

// ---------- helpers ----------

fun today(): String = LocalDate.now().toString()

fun Profile.todayIntake(): DayIntake = intake[today()] ?: DayIntake()

fun Profile.withIntake(f: (DayIntake) -> DayIntake): Profile {
    val cutoff = LocalDate.now().minusDays(120)
    val updated = (intake + (today() to f(todayIntake()))).filterKeys { key ->
        runCatching { LocalDate.parse(key).isAfter(cutoff) }.getOrDefault(false)
    }
    return copy(intake = updated)
}

fun Profile.addWater(ml: Int): Profile = withIntake { it.copy(waterMl = max(0, it.waterMl + ml)) }

fun Profile.logWeight(kg: Double): Profile = copy(
    health = health.copy(weightKg = kg),
    weights = (weights.filter { it.date != today() } + WeightEntry(today(), kg)).sortedBy { it.date }
)

fun Profile.startWeightKg(): Double = weights.minByOrNull { it.date }?.kg ?: health.weightKg

object Options {
    val proteins = listOf("Chicken", "Beef", "Turkey", "Pork", "Fish", "Shellfish", "Eggs", "Greek yogurt", "Whey", "Tofu", "Beans & lentils", "Plant protein")
    val carbs = listOf("Rice", "Pasta", "Potatoes", "Oats", "Bread", "Tortillas", "Noodles", "Quinoa", "Fruit")
    val cuisines = listOf("American", "Mexican", "Asian", "Italian", "Mediterranean", "Indian", "Southern")
    val accents = listOf(0xFF0A84FF, 0xFF5E5CE6, 0xFF32ADE6, 0xFF30B0C7, 0xFF34C759, 0xFFBF5AF2, 0xFFFF375F, 0xFFFF9F0A, 0xFF8E8E93)
    val emojis = listOf("💪", "🏋️", "🔥", "⚡", "🦍", "🐺", "🦁", "🏆", "🚀", "😎", "🥇", "🧠", "🐻", "🦅", "🌟", "🎯")
}

object Units {
    const val LB = 2.20462
    fun weight(kg: Double, imperial: Boolean, decimals: Boolean = false): String =
        if (imperial) { if (decimals) "%.1f lb".format(kg * LB) else "${(kg * LB).roundToInt()} lb" }
        else if (decimals) "%.1f kg".format(kg) else "${kg.roundToInt()} kg"

    fun weightNum(kg: Double, imperial: Boolean): Double = if (imperial) kg * LB else kg
    fun toKg(v: Double, imperial: Boolean): Double = if (imperial) v / LB else v
    fun unit(imperial: Boolean) = if (imperial) "lb" else "kg"

    fun height(cm: Double, imperial: Boolean): String {
        if (!imperial) return "${cm.roundToInt()} cm"
        val totalIn = (cm / 2.54).roundToInt()
        return "${totalIn / 12}′${totalIn % 12}″"
    }

    fun water(ml: Int, imperial: Boolean): String =
        if (imperial) "${(ml / 29.57).roundToInt()} oz" else "%.1f L".format(ml / 1000.0)
}
