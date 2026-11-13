package com.gainforge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gainforge.app.data.*
import com.gainforge.app.logic.*
import kotlin.math.roundToInt

@Composable
fun MealsScreen(profile: Profile, plan: Plan) {
    val p = LocalPalette.current
    var seg by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("For you") }
    var open by remember { mutableStateOf<Pair<Recipe, Double>?>(null) }
    val meals = remember(profile.food, profile.favorites, profile.planShuffle, plan.calories) { MealPlanner.plan(profile, plan.calories) }
    val intake = profile.todayIntake()

    ScreenColumn {
        item { LargeTitle("Meals") }
        item { Segmented(listOf("Today's plan", "Recipe book"), seg) { seg = it } }

        if (seg == 0) {
            item {
                GlassCard {
                    Caption("Planned for ${profile.name}")
                    Spacer(Modifier.height(6.dp))
                    val planned = meals.sumOf { it.kcal }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$planned", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = p.text, style = Tnum)
                        Text("  / ${plan.calories} kcal target", color = p.subtext, fontSize = 15.sp, modifier = Modifier.padding(bottom = 5.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("Protein", "${meals.sumOf { it.protein }}g", Modifier.weight(1f), "goal ${plan.protein}g")
                        StatTile("Carbs", "${meals.sumOf { (it.recipe.carbsG * it.servings).roundToInt() }}g", Modifier.weight(1f), "goal ${plan.carbs}g")
                        StatTile("Fat", "${meals.sumOf { (it.recipe.fat * it.servings).roundToInt() }}g", Modifier.weight(1f), "goal ${plan.fat}g")
                    }
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton("Shuffle meals", Modifier.fillMaxWidth(), Icons.Rounded.Shuffle) {
                        AppState.updateActive { it.copy(planShuffle = it.planShuffle + 1) }
                    }
                }
            }
            items(meals, key = { it.slot + it.recipe.id }) { m ->
                MealRow(m, eaten = m.recipe.id in intake.eaten, onToggle = { toggleEaten(m) }, onOpen = { open = m.recipe to m.servings })
            }
            if (meals.isEmpty()) item { GlassCard { Sub("No recipes fit your diet and allergy settings. Loosen them in Profile → Edit questionnaire.") } }
            item { Sub("Servings are scaled so the day adds up to your calorie target. Tick meals as you eat them.", Modifier.padding(horizontal = 4.dp)) }
        } else {
            item {
                GlassField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = "Search recipes or ingredients")
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("For you", "Favorites", "Breakfast", "Lunch & Dinner", "Snack", "Shake").forEach { f ->
                        SelectChip(f, f == filter) { filter = f }
                    }
                }
            }
            val list = Recipes.all
                .filter { MealPlanner.allowed(it, profile.food) }
                .filter { r ->
                    when (filter) {
                        "Favorites" -> r.id in profile.favorites
                        "For you" -> true
                        else -> r.type.label == filter
                    }
                }
                .filter { r -> query.isBlank() || (r.name + r.ingredients.joinToString() + r.cuisine).contains(query.trim(), ignoreCase = true) }
                .sortedByDescending { MealPlanner.score(it, profile) }
            items(list, key = { it.id }) { r -> RecipeRow(r, profile) { open = r to 1.0 } }
            if (list.isEmpty()) item { GlassCard { Sub(if (filter == "Favorites") "Tap the heart on a recipe to save it here." else "Nothing matches that search.") } }
        }
    }

    open?.let { (r, s) -> RecipeSheet(r, s, profile) { open = null } }
}

@Composable
fun MealRow(m: PlannedMeal, eaten: Boolean, onToggle: () -> Unit, onOpen: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().glass(highlighted = eaten).clickable(onClick = onOpen).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(p.accent.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
            Text(m.recipe.emoji, fontSize = 28.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Caption(m.slot)
            Text(m.recipe.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = p.text, maxLines = 2)
            Sub("${m.kcal} kcal · ${m.protein}g protein${if (m.servings != 1.0) " · ${fmtServ(m.servings)}×" else ""}")
            Sub(m.reason, color = p.accent)
        }
        Spacer(Modifier.width(8.dp))
        CheckDot(eaten, 30.dp, onToggle)
    }
}

@Composable
private fun RecipeRow(r: Recipe, profile: Profile, onOpen: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().glass().clickable(onClick = onOpen).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(p.accent.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
            Text(r.emoji, fontSize = 28.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(r.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = p.text)
            Sub("${r.cuisine} · ${r.minutes} min · ${r.kcal} kcal · ${r.protein}g P")
        }
        Column(horizontalAlignment = Alignment.End) {
            if (r.id in profile.favorites) Icon(Icons.Rounded.Favorite, null, tint = p.danger, modifier = Modifier.size(18.dp))
            Tag("${MealPlanner.match(r, profile)}%")
        }
    }
}

private fun fmtServ(s: Double) = if (s % 1.0 == 0.0) s.toInt().toString() else s.toString().trimEnd('0')

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeSheet(recipe: Recipe, startServings: Double, profile: Profile, onDismiss: () -> Unit) {
    val p = LocalPalette.current
    var servings by remember { mutableDoubleStateOf(startServings) }
    val fav = recipe.id in profile.favorites
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = p.sheet, contentColor = p.text) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(p.accent.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                    Text(recipe.emoji, fontSize = 40.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Title(recipe.name, size = 22.sp)
                    Sub("${recipe.cuisine} · ${recipe.minutes} min · ${recipe.type.label}")
                }
                IconButton(onClick = {
                    AppState.updateActive { it.copy(favorites = if (fav) it.favorites - recipe.id else it.favorites + recipe.id) }
                }) {
                    Icon(if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorite", tint = if (fav) p.danger else p.subtext)
                }
            }
            GlassCard(padding = 12.dp) {
                Stepper("Servings", "${fmtServ(servings)}×", { servings = (servings - 0.25).coerceAtLeast(0.25) }, { servings = (servings + 0.25).coerceAtMost(4.0) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("kcal", "${(recipe.kcal * servings).roundToInt()}", Modifier.weight(1f))
                StatTile("Protein", "${(recipe.protein * servings).roundToInt()}g", Modifier.weight(1f))
                StatTile("Carbs", "${(recipe.carbsG * servings).roundToInt()}g", Modifier.weight(1f))
                StatTile("Fat", "${(recipe.fat * servings).roundToInt()}g", Modifier.weight(1f))
            }
            Caption("Ingredients · 1 serving")
            if (servings != 1.0) Sub("Multiply each amount by ${fmtServ(servings)}.", color = p.accent)
            recipe.ingredients.forEach { Body("•  $it") }
            Caption("Steps")
            recipe.steps.forEachIndexed { i, s ->
                Row {
                    Text("${i + 1}", color = p.accent, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
                    Body(s)
                }
            }
            if (recipe.allergens.isNotEmpty()) Sub("Contains: " + recipe.allergens.joinToString { it.label })
            Spacer(Modifier.height(4.dp))
            val k = (recipe.kcal * servings).roundToInt()
            val pr = (recipe.protein * servings).roundToInt()
            PrimaryButton("Log $k kcal as eaten", Modifier.fillMaxWidth(), Icons.Rounded.Check) {
                AppState.updateActive { pf ->
                    pf.withIntake { it.copy(kcal = it.kcal + k, protein = it.protein + pr, eaten = if (recipe.id in it.eaten) it.eaten else it.eaten + recipe.id) }
                }
                onDismiss()
            }
        }
    }
}
