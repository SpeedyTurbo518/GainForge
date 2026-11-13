package com.gainforge.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gainforge.app.data.*
import com.gainforge.app.logic.GoalEngine
import com.gainforge.app.logic.GoalMode
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private const val STEPS = 10

@Composable
fun Onboarding(initial: Profile?, firstProfile: Boolean, onCancel: (() -> Unit)?, onDone: (Profile) -> Unit) {
    val p = LocalPalette.current
    val imperial = AppState.data.imperial
    var d by remember { mutableStateOf(initial ?: Profile(isFriend = !firstProfile)) }
    var step by rememberSaveable { mutableIntStateOf(0) }

    fun h(f: (Health) -> Health) { d = d.copy(health = f(d.health)) }
    fun food(f: (FoodPrefs) -> FoodPrefs) { d = d.copy(food = f(d.food)) }

    // Body measurement text fields (in the units being displayed)
    val h0 = d.health
    var age by remember { mutableStateOf(h0.age.toString()) }
    var cm by remember { mutableStateOf(h0.heightCm.roundToInt().toString()) }
    var ft by remember { mutableStateOf(((h0.heightCm / 2.54).roundToInt() / 12).toString()) }
    var inch by remember { mutableStateOf(((h0.heightCm / 2.54).roundToInt() % 12).toString()) }
    var weight by remember { mutableStateOf(Units.weightNum(h0.weightKg, imperial).roundToInt().toString()) }
    var bf by remember { mutableStateOf(h0.bodyFat?.roundToInt()?.toString() ?: "") }
    var rhr by remember { mutableStateOf(h0.restingHr?.toString() ?: "") }
    var ack by remember { mutableStateOf(initial != null) }

    /** Returns an error message, or the updated Health if every field is valid. */
    fun parseBody(): Pair<String?, Health?> {
        val a = age.toIntOrNull() ?: return "Enter your age" to null
        if (a !in 13..90) return "Age must be between 13 and 90" to null
        val heightCm = if (imperial) {
            val f = ft.toIntOrNull() ?: return "Enter your height" to null
            val i = inch.toIntOrNull() ?: 0
            (f * 12 + i) * 2.54
        } else cm.toDoubleOrNull() ?: return "Enter your height" to null
        if (heightCm !in 120.0..230.0) return "Height looks off — check the number" to null
        val wIn = weight.toDoubleOrNull() ?: return "Enter your weight" to null
        val kg = Units.toKg(wIn, imperial)
        if (kg !in 30.0..250.0) return "Weight looks off — check the number" to null
        val bodyFat = bf.toDoubleOrNull()?.takeIf { it in 3.0..60.0 }
        return null to d.health.copy(age = a, heightCm = heightCm, weightKg = kg, bodyFat = bodyFat)
    }

    val bodyError = if (step == 1) parseBody().first else null

    val canContinue = when (step) {
        0 -> d.name.isNotBlank()
        1 -> bodyError == null
        4 -> ack
        else -> true
    }

    fun finish() {
        val prevWeight = initial?.health?.weightKg
        var out = d.copy(name = d.name.trim(), health = d.health.copy(restingHr = rhr.toIntOrNull()))
        if (initial == null || prevWeight == null || kotlin.math.abs(prevWeight - out.health.weightKg) > 0.05) out = out.logWeight(out.health.weightKg)
        onDone(out)
    }

    BackHandler(enabled = step > 0 || onCancel != null) { if (step > 0) step-- else onCancel?.invoke() }

    AppBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (step > 0 || onCancel != null) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).glass(CircleShape).clickable { if (step > 0) step-- else onCancel?.invoke() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = p.accent) }
                    Spacer(Modifier.width(12.dp))
                }
                Bar((step + 1) / STEPS.toFloat(), Modifier.weight(1f), height = 6.dp)
                Spacer(Modifier.width(12.dp))
                Text("${step + 1}/$STEPS", color = p.subtext, fontSize = 13.sp, style = Tnum)
            }

            key(step) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (step) {
                        0 -> {
                            Text(if (initial == null) "Let's build your plan" else "Update your answers", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = p.text, letterSpacing = (-0.5).sp)
                            Sub("A few quick questions so the system can set an ideal goal, workouts and meals you'll actually enjoy. Everything stays on this phone.")
                            Spacer(Modifier.height(6.dp))
                            Caption("Who is this for?")
                            OptionCard("Me", "My own profile", !d.isFriend, "🙋") { d = d.copy(isFriend = false) }
                            OptionCard("A friend", "Set up a profile for someone you train with", d.isFriend, "🤝") { d = d.copy(isFriend = true) }
                            Caption("Name")
                            GlassField(d.name, { d = d.copy(name = it.take(24)) }, Modifier.fillMaxWidth(), placeholder = if (d.isFriend) "Friend's name" else "Your name")
                            Caption("Pick an avatar")
                            ChipGroup(Options.emojis, { it == d.emoji }) { d = d.copy(emoji = it) }
                        }
                        1 -> {
                            StepTitle("Body basics", "Used to work out your calories and ideal weight.")
                            Caption("Sex")
                            Segmented(Sex.entries.map { it.label }, d.health.sex.ordinal) { i -> h { it.copy(sex = Sex.entries[i]) } }
                            Caption("Units")
                            Segmented(listOf("lb / ft", "kg / cm"), if (imperial) 0 else 1) { i ->
                                val toImp = i == 0
                                if (toImp != imperial) {
                                    weight.toDoubleOrNull()?.let { w -> weight = (if (toImp) w * Units.LB else w / Units.LB).roundToInt().toString() }
                                    val curCm = if (imperial) ((ft.toIntOrNull() ?: 0) * 12 + (inch.toIntOrNull() ?: 0)) * 2.54 else cm.toDoubleOrNull() ?: 0.0
                                    val totalIn = (curCm / 2.54).roundToInt()
                                    cm = curCm.roundToInt().toString(); ft = (totalIn / 12).toString(); inch = (totalIn % 12).toString()
                                    AppState.update { it.copy(imperial = toImp) }
                                }
                            }
                            Caption("Age")
                            GlassField(age, { age = it.filter(Char::isDigit).take(2) }, Modifier.fillMaxWidth(), suffix = "years", keyboardType = KeyboardType.Number)
                            Caption("Height")
                            if (imperial) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                GlassField(ft, { ft = it.filter(Char::isDigit).take(1) }, Modifier.weight(1f), suffix = "ft", keyboardType = KeyboardType.Number)
                                GlassField(inch, { inch = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f), suffix = "in", keyboardType = KeyboardType.Number)
                            } else GlassField(cm, { cm = it.filter(Char::isDigit).take(3) }, Modifier.fillMaxWidth(), suffix = "cm", keyboardType = KeyboardType.Number)
                            Caption("Weight")
                            GlassField(weight, { weight = it.filter { c -> c.isDigit() || c == '.' }.take(5) }, Modifier.fillMaxWidth(), suffix = Units.unit(imperial), keyboardType = KeyboardType.Decimal)
                            Caption("Body fat % (optional)")
                            GlassField(bf, { bf = it.filter { c -> c.isDigit() || c == '.' }.take(4) }, Modifier.fillMaxWidth(), placeholder = "Not sure? Leave blank and we'll estimate", suffix = "%", keyboardType = KeyboardType.Decimal)
                            if (bodyError != null && (weight.isNotEmpty() && age.isNotEmpty())) Sub(bodyError, color = p.warn)
                        }
                        2 -> {
                            StepTitle("Your lifestyle", "Movement outside the gym changes how much you need to eat.")
                            Caption("Daily activity")
                            Activity.entries.forEach { a -> OptionCard(a.label, a.detail, d.health.activity == a) { h { it.copy(activity = a) } } }
                            Caption("Lifting experience")
                            Experience.entries.forEach { e -> OptionCard(e.label, e.detail, d.health.experience == e) { h { it.copy(experience = e) } } }
                        }
                        3 -> {
                            StepTitle("Training setup", "We'll build a program around your schedule.")
                            Caption("Days per week")
                            ChipGroup((2..6).map { "$it days" }, { it == "${d.health.days} days" }) { s -> h { it.copy(days = s.first().digitToInt()) } }
                            Caption("Time per workout")
                            ChipGroup(listOf(30, 45, 60, 75, 90).map { "$it min" }, { it == "${d.health.sessionMin} min" }) { s -> h { it.copy(sessionMin = s.substringBefore(' ').toInt()) } }
                            Caption("Equipment")
                            Equipment.entries.forEach { e -> OptionCard(e.label, e.detail, d.health.equipment == e) { h { it.copy(equipment = e) } } }
                        }
                        4 -> {
                            StepTitle("Health check", "This keeps your plan safe. Exercises are swapped around injuries.")
                            Caption("Anything we should know about?")
                            ChipGroup(Condition.entries.map { it.label }, { lbl -> d.health.conditions.any { it.label == lbl } }) { lbl ->
                                val c = Condition.entries.first { it.label == lbl }
                                h { it.copy(conditions = if (c in it.conditions) it.conditions - c else it.conditions + c) }
                            }
                            Caption("Sleep")
                            GlassCard(padding = 12.dp) {
                                Stepper("Hours per night", "%.1f h".format(d.health.sleepHours),
                                    { h { it.copy(sleepHours = (it.sleepHours - 0.5).coerceAtLeast(3.0)) } },
                                    { h { it.copy(sleepHours = (it.sleepHours + 0.5).coerceAtMost(12.0)) } })
                            }
                            Caption("Resting heart rate (optional)")
                            GlassField(rhr, { rhr = it.filter(Char::isDigit).take(3) }, Modifier.fillMaxWidth(), placeholder = "e.g. 62", suffix = "bpm", keyboardType = KeyboardType.Number)
                            Spacer(Modifier.height(4.dp))
                            Row(
                                Modifier.fillMaxWidth().glass(highlighted = ack).clickable { ack = !ack }.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CheckDot(ack)
                                Spacer(Modifier.width(12.dp))
                                Sub("I understand GainForge gives general fitness guidance, not medical advice, and I'll check with a doctor if I have a health condition.", color = p.text)
                            }
                        }
                        5 -> {
                            StepTitle("Your goal look", "Pick the physique you're going for. The system sets your ideal weight from it.")
                            Physique.entries.forEach { ph ->
                                OptionCard(ph.title, "${ph.subtitle}\n${ph.detail}", d.goal.physique == ph, ph.emoji) { d = d.copy(goal = d.goal.copy(physique = ph)) }
                            }
                            Caption("Gain pace")
                            Segmented(Pace.entries.map { it.label }, d.goal.pace.ordinal) { i -> d = d.copy(goal = d.goal.copy(pace = Pace.entries[i])) }
                            Sub(d.goal.pace.detail)
                        }
                        6 -> {
                            StepTitle("Diet & allergies", "Recipes that don't fit are never shown.")
                            Caption("Diet")
                            ChipGroup(Diet.entries.map { it.label }, { it == d.food.diet.label }) { lbl -> food { it.copy(diet = Diet.entries.first { e -> e.label == lbl }) } }
                            Caption("Allergies / avoid")
                            ChipGroup(Allergen.entries.map { it.label }, { lbl -> d.food.allergies.any { it.label == lbl } }) { lbl ->
                                val a = Allergen.entries.first { it.label == lbl }
                                food { it.copy(allergies = if (a in it.allergies) it.allergies - a else it.allergies + a) }
                            }
                        }
                        7 -> {
                            StepTitle("Foods ${if (d.isFriend) d.name.ifBlank { "they" } + " likes" else "you like"}", "Tap everything that sounds good. Meals are picked from these.")
                            Caption("Proteins")
                            ChipGroup(Options.proteins, { it in d.food.proteins }) { s -> food { it.copy(proteins = it.proteins.toggle(s)) } }
                            Caption("Carbs")
                            ChipGroup(Options.carbs, { it in d.food.carbs }) { s -> food { it.copy(carbs = it.carbs.toggle(s)) } }
                            Caption("Cuisines")
                            ChipGroup(Options.cuisines, { it in d.food.cuisines }) { s -> food { it.copy(cuisines = it.cuisines.toggle(s)) } }
                            Caption("Foods to avoid (comma separated)")
                            GlassField(d.food.dislikes, { v -> food { it.copy(dislikes = v.take(120)) } }, Modifier.fillMaxWidth(), placeholder = "e.g. mushrooms, olives")
                        }
                        8 -> {
                            StepTitle("Eating habits", "Gaining weight is mostly about eating enough, consistently.")
                            Caption("Appetite")
                            Appetite.entries.forEach { a -> OptionCard(a.label, a.detail, d.food.appetite == a) { food { it.copy(appetite = a) } } }
                            Caption("Cooking")
                            CookTime.entries.forEach { c -> OptionCard(c.label, c.detail, d.food.cookTime == c) { food { it.copy(cookTime = c) } } }
                            Caption("Meals per day")
                            ChipGroup((3..6).map { "$it meals" }, { it == "${d.food.mealsPerDay} meals" }) { s -> food { it.copy(mealsPerDay = s.first().digitToInt()) } }
                        }
                        else -> ResultStep(d, imperial)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            PrimaryButton(
                if (step == STEPS - 1) "Let's go" else "Continue",
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                enabled = canContinue
            ) {
                if (step == 1) parseBody().second?.let { hh -> d = d.copy(health = hh) }
                if (step == STEPS - 1) finish() else step++
            }
        }
    }
}

private fun Set<String>.toggle(s: String) = if (s in this) this - s else this + s

@Composable
private fun StepTitle(title: String, sub: String) {
    Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = LocalPalette.current.text, letterSpacing = (-0.5).sp)
    Sub(sub)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun ResultStep(d: Profile, imperial: Boolean) {
    val p = LocalPalette.current
    val plan = remember(d) { GoalEngine.compute(d) }
    StepTitle("Your ideal goal", "Calculated from your body, experience and the ${d.goal.physique.title.lowercase()} look.")
    GlassCard(highlighted = true) {
        Caption("Target weight")
        Text(Units.weight(plan.targetKg, imperial), fontSize = 52.sp, fontWeight = FontWeight.Bold, color = p.accent, letterSpacing = (-1).sp)
        val diff = plan.targetKg - d.health.weightKg
        Body(
            when (plan.mode) {
                GoalMode.BULK -> "+${Units.weight(diff, imperial)} from ${Units.weight(d.health.weightKg, imperial)} · about ${plan.weeks} weeks (${plan.eta.format(DateTimeFormatter.ofPattern("MMM yyyy"))})"
                GoalMode.RECOMP -> "Build muscle at about your current weight (recomp)"
                GoalMode.MAINTAIN -> "Maintain and build strength"
            }
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("Calories", "${plan.calories}", Modifier.weight(1f), "kcal / day")
        StatTile("Protein", "${plan.protein} g", Modifier.weight(1f), "per day")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("Carbs", "${plan.carbs} g", Modifier.weight(1f))
        StatTile("Fat", "${plan.fat} g", Modifier.weight(1f))
    }
    GlassCard {
        Caption("How we got there")
        Spacer(Modifier.height(6.dp))
        Sub("Body fat ${"%.0f".format(plan.bodyFat)}%${if (plan.bodyFatEstimated) " (estimated)" else ""} · lean mass ${Units.weight(plan.leanKg, imperial)}")
        Sub("Muscle index (FFMI) ${"%.1f".format(plan.ffmi)} → ${"%.1f".format(plan.targetFfmi)} at ${plan.targetBodyFat.roundToInt()}% body fat")
        Sub("Burn ≈ ${plan.tdee} kcal/day + ${plan.surplus} kcal surplus for growth")
    }
    plan.warnings.forEach { w ->
        GlassCard { Body("⚠️  $w", color = p.warn) }
    }
    Sub("You can change your goal any time on the Goals tab.", color = p.subtext)
}
