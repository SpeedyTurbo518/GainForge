package com.gainforge.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gainforge.app.data.*
import com.gainforge.app.logic.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt

private val tips = listOf(
    "Progressive overload: try to add 1 rep or a little weight each week.",
    "Liquid calories count. A glass of whole milk is ~150 kcal and 8 g protein.",
    "Sleep 7–9 hours. Growth hormone peaks during deep sleep.",
    "Eat a protein-rich meal within a couple of hours after training.",
    "Struggling to eat? Add olive oil, nut butter or granola to meals you already like.",
    "Leave 1–2 reps in the tank on most sets; push closer on the last set.",
    "Creatine monohydrate (3–5 g/day) is one of the best-researched supplements — ask your doctor if it's right for you.",
    "Weigh yourself in the morning after the bathroom, before eating, for consistent numbers."
)

@Composable
fun TodayScreen(profile: Profile, plan: Plan, program: Program, imperial: Boolean, go: (AppTab) -> Unit) {
    val p = LocalPalette.current
    val intake = profile.todayIntake()
    val meals = remember(profile.food, profile.favorites, profile.planShuffle, plan.calories) { MealPlanner.plan(profile, plan.calories) }
    val day = program.days[profile.programDay.mod(program.days.size)]
    val doneToday = profile.workouts.any { it.date == today() }
    var showFood by remember { mutableStateOf(false) }
    var openRecipe by remember { mutableStateOf<PlannedMeal?>(null) }
    val hour = LocalTime.now().hour
    val greeting = when (hour) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }

    ScreenColumn {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Caption(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")))
                    Text("$greeting,", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = p.subtext)
                    LargeTitle(profile.name)
                }
                Avatar(profile, 52.dp)
            }
        }

        // Fuel
        item {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(intake.kcal / plan.calories.toFloat(), Modifier.size(118.dp), stroke = 13.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${intake.kcal}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = p.text, style = Tnum)
                            Sub("of ${plan.calories}")
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Caption("Today's fuel")
                        MacroBar("Protein", intake.protein, plan.protein, "g", p.accent)
                        MacroBar("Water", (intake.waterMl / 100.0).roundToInt() * 100, plan.waterMl, "ml", p.mint)
                    }
                }
                Spacer(Modifier.height(14.dp))
                val left = max(0, plan.calories - intake.kcal)
                Sub(if (left > 0) "$left kcal to go — that's about ${(left / 650.0).let { if (it < 1) "a snack" else "${it.roundToInt()} meal${if (it >= 1.5) "s" else ""}" }}." else "Calorie goal hit. Nice work! 🎉")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Water +250", Modifier.weight(1f), Icons.Rounded.WaterDrop, p.mint) { AppState.updateActive { it.addWater(250) } }
                    SecondaryButton("Add food", Modifier.weight(1f), Icons.Rounded.Add) { showFood = true }
                }
            }
        }

        // Workout
        item {
            GlassCard(highlighted = WorkoutSession.active, onClick = { go(AppTab.TRAIN) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Caption(if (doneToday) "Trained today ✓" else "Today's workout")
                        Title(day.title, size = 22.sp)
                        Sub("${day.focus} · ${day.exercises.size} exercises · ~${day.estMinutes} min")
                    }
                    Icon(Icons.Rounded.FitnessCenter, p.accent)
                }
                Spacer(Modifier.height(14.dp))
                PrimaryButton(
                    if (WorkoutSession.active) "Continue workout" else if (doneToday) "Train again" else "Start workout",
                    Modifier.fillMaxWidth(), Icons.Rounded.PlayArrow
                ) {
                    if (!WorkoutSession.active) WorkoutSession.start(profile.programDay.mod(program.days.size))
                    go(AppTab.TRAIN)
                }
            }
        }

        // Goal
        item {
            val start = profile.startWeightKg()
            val now = profile.health.weightKg
            val progress = if (plan.targetKg > start) ((now - start) / (plan.targetKg - start)).toFloat() else 1f
            GlassCard(onClick = { go(AppTab.GOALS) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Caption("${profile.goal.physique.emoji}  ${profile.goal.physique.title} goal")
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(Units.weight(now, imperial), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = p.text)
                            Text("  →  ${Units.weight(plan.targetKg, imperial)}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = p.accent, modifier = Modifier.padding(bottom = 2.dp))
                        }
                    }
                    Icon(Icons.Rounded.Flag, p.accent)
                }
                Spacer(Modifier.height(10.dp))
                Bar(progress)
                Spacer(Modifier.height(8.dp))
                Sub(
                    when (plan.mode) {
                        GoalMode.BULK -> "${(progress * 100).roundToInt().coerceIn(0, 100)}% there · ~${plan.weeks} weeks to go · ETA ${plan.eta.format(DateTimeFormatter.ofPattern("MMM yyyy"))}"
                        GoalMode.RECOMP -> "Recomp mode: build muscle at about your current weight"
                        GoalMode.MAINTAIN -> "At your target — maintain and keep getting stronger"
                    }
                )
            }
        }

        // Meals
        val upcoming = meals.filter { it.recipe.id !in intake.eaten }.take(3)
        item { Caption(if (upcoming.isEmpty()) "All planned meals eaten 🎉" else "Up next to eat", Modifier.padding(start = 4.dp, top = 4.dp)) }
        items(upcoming, key = { it.recipe.id }) { m ->
            MealRow(m, eaten = false, onToggle = { toggleEaten(m) }, onOpen = { openRecipe = m })
        }
        if (meals.isEmpty()) item { GlassCard { Sub("No recipes match your diet + allergy settings. Edit them in Profile → Edit questionnaire.") } }

        // Tip
        item {
            GlassCard {
                Caption("💡  Tip of the day")
                Spacer(Modifier.height(6.dp))
                Body(tips[LocalDate.now().dayOfYear % tips.size])
            }
        }
    }

    if (showFood) QuickFoodDialog(onDismiss = { showFood = false }) { kcal, protein ->
        AppState.updateActive { pr -> pr.withIntake { it.copy(kcal = it.kcal + kcal, protein = it.protein + protein) } }
        showFood = false
    }
    openRecipe?.let { m -> RecipeSheet(m.recipe, m.servings, profile) { openRecipe = null } }
}

@Composable
private fun Icon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color) =
    androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp))

fun toggleEaten(m: PlannedMeal) = AppState.updateActive { pr ->
    pr.withIntake { di ->
        if (m.recipe.id in di.eaten) di.copy(kcal = max(0, di.kcal - m.kcal), protein = max(0, di.protein - m.protein), eaten = di.eaten - m.recipe.id)
        else di.copy(kcal = di.kcal + m.kcal, protein = di.protein + m.protein, eaten = di.eaten + m.recipe.id)
    }
}

@Composable
fun QuickFoodDialog(onDismiss: () -> Unit, onAdd: (Int, Int) -> Unit) {
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add food") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Sub("Log something that isn't in your plan.")
                GlassField(kcal, { kcal = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth(), placeholder = "Calories", suffix = "kcal", keyboardType = KeyboardType.Number)
                GlassField(protein, { protein = it.filter(Char::isDigit).take(3) }, Modifier.fillMaxWidth(), placeholder = "Protein (optional)", suffix = "g", keyboardType = KeyboardType.Number)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(150 to 8, 300 to 25, 500 to 30).forEach { (k, pr) ->
                        SelectChip("$k kcal", false) { kcal = k.toString(); protein = pr.toString() }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { kcal.toIntOrNull()?.let { onAdd(it, protein.toIntOrNull() ?: 0) } }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
