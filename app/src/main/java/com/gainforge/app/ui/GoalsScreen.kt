package com.gainforge.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gainforge.app.data.*
import com.gainforge.app.logic.GoalMode
import com.gainforge.app.logic.Plan
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun GoalsScreen(profile: Profile, plan: Plan, imperial: Boolean) {
    val p = LocalPalette.current
    var weightInput by remember { mutableStateOf("") }
    var customInput by remember(profile.goal.customTargetKg, imperial) {
        mutableStateOf(profile.goal.customTargetKg?.let { Units.weightNum(it, imperial).roundToInt().toString() } ?: "")
    }

    ScreenColumn {
        item { LargeTitle("Goals") }

        item {
            GlassCard(highlighted = true) {
                Caption("Your ideal goal · set by the system")
                Text(Units.weight(plan.targetKg, imperial), fontSize = 56.sp, fontWeight = FontWeight.Bold, color = p.accent, letterSpacing = (-1.5).sp)
                val diff = plan.targetKg - profile.health.weightKg
                Body(
                    when (plan.mode) {
                        GoalMode.BULK -> "+${Units.weight(diff, imperial)} from ${Units.weight(profile.health.weightKg, imperial)} at about ${Units.weight(plan.weeklyGainKg, imperial, true)}/week"
                        GoalMode.RECOMP -> "Recomp: build muscle at about ${Units.weight(profile.health.weightKg, imperial)} while body fat drops"
                        GoalMode.MAINTAIN -> "You're at your target. Maintain and keep getting stronger."
                    }
                )
                if (plan.mode != GoalMode.MAINTAIN) {
                    Spacer(Modifier.height(4.dp))
                    Sub("≈ ${plan.weeks} weeks · ETA ${plan.eta.format(DateTimeFormatter.ofPattern("MMMM yyyy"))}")
                }
                if (profile.goal.customTargetKg != null) {
                    Spacer(Modifier.height(4.dp))
                    Sub("Using your own target. System suggestion: ${Units.weight(plan.systemTargetKg, imperial)}", color = p.warn)
                }
            }
        }

        item { Caption("Physique", Modifier.padding(start = 4.dp)) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 4.dp)) {
                items(Physique.entries) { ph ->
                    val sel = ph == profile.goal.physique
                    Column(
                        Modifier.width(150.dp).glass(RoundedCornerShape(22.dp), highlighted = sel)
                            .clickable { AppState.updateActive { it.copy(goal = it.goal.copy(physique = ph)) } }
                            .padding(14.dp)
                    ) {
                        Text(ph.emoji, fontSize = 30.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(ph.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = if (sel) p.accent else p.text)
                        Sub(ph.subtitle)
                    }
                }
            }
        }
        item { Sub(profile.goal.physique.detail, Modifier.padding(horizontal = 4.dp)) }

        item {
            GlassCard {
                Caption("Gain pace")
                Spacer(Modifier.height(8.dp))
                Segmented(Pace.entries.map { it.label }, profile.goal.pace.ordinal) { i -> AppState.updateActive { it.copy(goal = it.goal.copy(pace = Pace.entries[i])) } }
                Spacer(Modifier.height(6.dp))
                Sub(profile.goal.pace.detail)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Body("Set my own target weight", weight = FontWeight.Medium)
                        Sub("Override the system's suggestion")
                    }
                    Switch(
                        checked = profile.goal.customTargetKg != null,
                        onCheckedChange = { on ->
                            AppState.updateActive { it.copy(goal = it.goal.copy(customTargetKg = if (on) plan.systemTargetKg else null)) }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = p.accent)
                    )
                }
                if (profile.goal.customTargetKg != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassField(customInput, { customInput = it.filter { c -> c.isDigit() || c == '.' }.take(5) }, Modifier.weight(1f), suffix = Units.unit(imperial), keyboardType = KeyboardType.Decimal)
                        Spacer(Modifier.width(10.dp))
                        SelectChip("Save", true) {
                            customInput.toDoubleOrNull()?.let { v ->
                                val kg = Units.toKg(v, imperial).coerceIn(35.0, 200.0)
                                AppState.updateActive { it.copy(goal = it.goal.copy(customTargetKg = kg)) }
                            }
                        }
                    }
                }
            }
        }

        item {
            GlassCard {
                Caption("Daily targets")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${plan.calories}", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = p.text, style = Tnum)
                    Text("  kcal / day", color = p.subtext, modifier = Modifier.padding(bottom = 7.dp))
                }
                Spacer(Modifier.height(10.dp))
                val total = (plan.protein * 4 + plan.carbs * 4 + plan.fat * 9).toFloat()
                MacroLine("Protein", plan.protein, plan.protein * 4 / total, p.accent)
                MacroLine("Carbs", plan.carbs, plan.carbs * 4 / total, p.mint)
                MacroLine("Fat", plan.fat, plan.fat * 9 / total, p.warn)
                Spacer(Modifier.height(6.dp))
                Sub("Water: ${Units.water(plan.waterMl, imperial)} · Burn ≈ ${plan.tdee} kcal + ${plan.surplus} kcal surplus")
            }
        }

        item {
            GlassCard {
                Caption("Body stats")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("BMI", "%.1f".format(plan.bmi), Modifier.weight(1f))
                    StatTile("Body fat", "${plan.bodyFat.roundToInt()}%", Modifier.weight(1f), if (plan.bodyFatEstimated) "estimated" else "entered")
                    StatTile("Lean", Units.weight(plan.leanKg, imperial), Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("FFMI", "%.1f → %.1f".format(plan.ffmi, plan.targetFfmi), Modifier.weight(1.4f), "muscle index")
                    StatTile("BMR", "${plan.bmr}", Modifier.weight(1f), "kcal at rest")
                }
                Spacer(Modifier.height(8.dp))
                Sub("FFMI = lean mass ÷ height². Around 20 looks fit, 22–23 looks clearly muscular, and 25 is roughly the top end most people reach without drugs.")
            }
        }

        item {
            GlassCard {
                Caption("Weight progress")
                Spacer(Modifier.height(10.dp))
                WeightChart(profile.weights, plan.targetKg, imperial)
                Spacer(Modifier.height(10.dp))
                val ws = profile.weights.sortedBy { it.date }
                val weekAgo = ws.lastOrNull { LocalDate.parse(it.date).isBefore(LocalDate.now().minusDays(6)) }
                if (weekAgo != null) {
                    val change = profile.health.weightKg - weekAgo.kg
                    Sub("Last 7 days: ${if (change >= 0) "+" else "−"}${Units.weight(kotlin.math.abs(change), imperial, true)} (goal +${Units.weight(plan.weeklyGainKg, imperial, true)})",
                        color = if (change >= plan.weeklyGainKg * 0.5) p.good else p.warn)
                    Spacer(Modifier.height(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassField(weightInput, { weightInput = it.filter { c -> c.isDigit() || c == '.' }.take(5) }, Modifier.weight(1f),
                        placeholder = "Today's weight", suffix = Units.unit(imperial), keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.width(10.dp))
                    PrimaryButton("Log", Modifier.width(90.dp)) {
                        weightInput.toDoubleOrNull()?.let { v ->
                            val kg = Units.toKg(v, imperial)
                            if (kg in 30.0..250.0) { AppState.updateActive { it.logWeight(kg) }; weightInput = "" }
                        }
                    }
                }
            }
        }

        if (plan.warnings.isNotEmpty() || plan.notes.isNotEmpty()) item {
            GlassCard {
                Caption("Coach notes")
                Spacer(Modifier.height(8.dp))
                plan.warnings.forEach { Body("⚠️  $it", color = p.warn); Spacer(Modifier.height(8.dp)) }
                plan.notes.forEach { Body("•  $it"); Spacer(Modifier.height(6.dp)) }
            }
        }
        item { Sub("GainForge gives general fitness and nutrition guidance, not medical advice.", Modifier.padding(horizontal = 4.dp)) }
    }
}

@Composable
private fun MacroLine(label: String, grams: Int, frac: Float, color: Color) {
    Column(Modifier.padding(vertical = 5.dp)) {
        Row {
            Body(label, Modifier.weight(1f), weight = FontWeight.Medium)
            Text("$grams g · ${(frac * 100).roundToInt()}%", color = LocalPalette.current.subtext, fontSize = 14.sp, style = Tnum)
        }
        Spacer(Modifier.height(5.dp))
        Bar(frac, color = color)
    }
}

@Composable
private fun WeightChart(entries: List<WeightEntry>, targetKg: Double, imperial: Boolean) {
    val p = LocalPalette.current
    val data = entries.sortedBy { it.date }.takeLast(40)
    if (data.size < 2) {
        Sub("Log your weight a few times a week to see your trend here.")
        if (data.isEmpty()) return
    }
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        val vals = data.map { it.kg }
        val minV = minOf(vals.min(), targetKg) - 1.5
        val maxV = maxOf(vals.max(), targetKg) + 1.5
        fun y(v: Double) = (size.height - (v - minV) / (maxV - minV) * size.height).toFloat()
        fun x(i: Int) = if (data.size == 1) size.width / 2 else i * size.width / (data.size - 1)

        drawLine(p.accent.copy(alpha = .55f), Offset(0f, y(targetKg)), Offset(size.width, y(targetKg)), 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))

        val line = Path()
        data.forEachIndexed { i, e -> if (i == 0) line.moveTo(x(i), y(e.kg)) else line.lineTo(x(i), y(e.kg)) }
        val fill = Path().apply {
            addPath(line)
            lineTo(x(data.lastIndex), size.height)
            lineTo(x(0), size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(p.accent.copy(alpha = .28f), Color.Transparent)))
        drawPath(line, p.accent, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        data.forEachIndexed { i, e ->
            drawCircle(Color.White, 4.5.dp.toPx(), Offset(x(i), y(e.kg)))
            drawCircle(p.accent, 3.dp.toPx(), Offset(x(i), y(e.kg)))
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Sub("Start ${Units.weight(data.first().kg, imperial)}", Modifier.weight(1f))
        Sub("Target ${Units.weight(targetKg, imperial)}", color = p.accent)
    }
}
