package com.gainforge.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gainforge.app.data.*
import com.gainforge.app.logic.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun TrainScreen(profile: Profile, program: Program, imperial: Boolean) {
    val p = LocalPalette.current
    var seg by rememberSaveable { mutableIntStateOf(0) }
    var confirmCancel by remember { mutableStateOf(false) }
    val upNext = profile.programDay.mod(program.days.size)
    val dayIdx = (if (WorkoutSession.active) WorkoutSession.dayIndex else upNext).coerceIn(0, program.days.lastIndex)
    val day = program.days[dayIdx]

    ScreenColumn {
        item { LargeTitle("Train") }
        item { Segmented(listOf("Workout", "Program", "History"), seg) { seg = it } }

        when (seg) {
            0 -> {
                item {
                    if (WorkoutSession.active) {
                        val done = WorkoutSession.done.count { it.value }
                        GlassCard(highlighted = true) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Caption("In progress")
                                    Title(day.title, size = 22.sp)
                                }
                                Text(fmtClock(TimerEngine.now - WorkoutSession.startedAt), fontSize = 30.sp, fontWeight = FontWeight.Light, color = p.text, style = Tnum)
                            }
                            Spacer(Modifier.height(12.dp))
                            Bar(done / day.totalSets.toFloat())
                            Spacer(Modifier.height(6.dp))
                            Sub("$done of ${day.totalSets} sets done · tick a set to start its rest timer")
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SecondaryButton("Cancel", Modifier.weight(1f), color = p.danger) { confirmCancel = true }
                                PrimaryButton("Finish", Modifier.weight(1f), Icons.Rounded.Check) {
                                    val log = WorkoutSession.buildLog(day, imperial)
                                    AppState.updateActive { it.copy(workouts = it.workouts + log, programDay = it.programDay + 1) }
                                    WorkoutSession.cancel()
                                    seg = 2
                                }
                            }
                        }
                    } else {
                        GlassCard {
                            Caption("Up next · Day ${dayIdx + 1} of ${program.days.size}")
                            Title(day.title, size = 26.sp)
                            Sub("${day.focus} · ${day.totalSets} sets · ~${day.estMinutes} min")
                            if (profile.workouts.any { it.date == today() }) {
                                Spacer(Modifier.height(6.dp))
                                Tag("Already trained today ✓", p.good)
                            }
                            Spacer(Modifier.height(14.dp))
                            PrimaryButton("Start workout", Modifier.fillMaxWidth(), Icons.Rounded.PlayArrow) { WorkoutSession.start(dayIdx) }
                            Spacer(Modifier.height(8.dp))
                            SecondaryButton("Skip to next day", Modifier.fillMaxWidth(), Icons.Rounded.SkipNext) {
                                AppState.updateActive { it.copy(programDay = it.programDay + 1) }
                            }
                        }
                    }
                }
                itemsIndexed(day.exercises) { ei, ex -> ExerciseCard(ei, ex, imperial) }
                day.finisher?.let { f -> item { GlassCard { Caption("🔥  Finisher"); Spacer(Modifier.height(4.dp)); Body(f) } } }
                item {
                    GlassCard {
                        Caption("How to progress")
                        Spacer(Modifier.height(4.dp))
                        Sub("When you hit the top of the rep range on every set, add weight next time (about 2.5–5 lb / 1–2 kg for upper body, 5–10 lb / 2–5 kg for legs). Stop each set with 1–2 good reps left.")
                    }
                }
            }
            1 -> {
                item {
                    GlassCard {
                        Caption("Your program")
                        Title(program.name)
                        Spacer(Modifier.height(4.dp))
                        Sub("Built from your goal, ${profile.health.experience.label.lowercase()} experience, ${profile.health.equipment.label.lowercase()} and ${profile.health.sessionMin}-min sessions. Rotate through the days in order — rest when you need to.")
                    }
                }
                itemsIndexed(program.days) { i, d ->
                    GlassCard(highlighted = i == upNext, onClick = if (!WorkoutSession.active) ({ AppState.updateActive { it.copy(programDay = it.programDay - upNext + i) } }) else null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Caption("Day ${i + 1}")
                                Title(d.title)
                            }
                            if (i == upNext) Tag("Up next")
                        }
                        Sub(d.focus + " · ~${d.estMinutes} min")
                        Spacer(Modifier.height(8.dp))
                        d.exercises.forEach { ex -> Body("•  ${ex.name}  —  ${ex.sets}×${ex.reps}") }
                        if (i != upNext && !WorkoutSession.active) {
                            Spacer(Modifier.height(6.dp))
                            Sub("Tap to do this one next", color = p.accent)
                        }
                    }
                }
            }
            else -> {
                val logs = profile.workouts.sortedByDescending { it.date }
                val weekAgo = LocalDate.now().minusDays(6)
                val thisWeek = logs.count { runCatching { !LocalDate.parse(it.date).isBefore(weekAgo) }.getOrDefault(false) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("This week", "$thisWeek / ${profile.health.days}", Modifier.weight(1f), "workouts")
                        StatTile("All time", "${logs.size}", Modifier.weight(1f), "workouts")
                        StatTile("Streak", "${streakWeeks(logs, profile.health.days)}", Modifier.weight(1f), "weeks")
                    }
                }
                if (logs.isEmpty()) item { GlassCard { Sub("Finished workouts show up here.") } }
                items(logs.take(60)) { log ->
                    GlassCard(padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(log.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = p.text)
                                Sub(
                                    LocalDate.parse(log.date).format(DateTimeFormatter.ofPattern("EEE, MMM d")) +
                                        " · ${log.durationSec / 60} min · ${log.sets} sets" +
                                        if (log.volumeKg > 0) " · ${"%,d".format(Units.weightNum(log.volumeKg, imperial).roundToInt())} ${Units.unit(imperial)} lifted" else ""
                                )
                            }
                            Icon(Icons.Rounded.CheckCircle, p.good)
                        }
                    }
                }
            }
        }
    }

    if (confirmCancel) AlertDialog(
        onDismissRequest = { confirmCancel = false },
        title = { Text("Cancel this workout?") },
        text = { Text("Sets you've ticked won't be saved.") },
        confirmButton = { TextButton(onClick = { WorkoutSession.cancel(); confirmCancel = false }) { Text("Cancel workout") } },
        dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep going") } }
    )
}

@Composable
private fun Icon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color) =
    androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))

private fun streakWeeks(logs: List<WorkoutLog>, target: Int): Int {
    if (logs.isEmpty()) return 0
    val goal = (target - 1).coerceAtLeast(1)
    var weeks = 0
    var end = LocalDate.now()
    var current = true // the current week may still be in progress, so it doesn't break the streak
    while (weeks < 520) {
        val start = end.minusDays(6)
        val n = logs.count { runCatching { val d = LocalDate.parse(it.date); !d.isBefore(start) && !d.isAfter(end) }.getOrDefault(false) }
        if (n >= goal) weeks++ else if (!current) break
        current = false
        end = start.minusDays(1)
    }
    return weeks
}

@Composable
private fun ExerciseCard(ei: Int, ex: PlannedExercise, imperial: Boolean) {
    val p = LocalPalette.current
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Title(ex.name, size = 17.sp)
                Sub("${ex.sets} sets × ${ex.reps} · rest ${ex.restSec}s")
            }
            Tag(ex.muscle)
        }
        ex.note?.let { Spacer(Modifier.height(4.dp)); Sub("ⓘ  $it", color = p.warn) }
        if (WorkoutSession.active) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Caption("Set", Modifier.width(34.dp))
                Caption(Units.unit(imperial), Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Caption("Reps", Modifier.weight(1f))
                Spacer(Modifier.width(38.dp))
            }
            repeat(ex.sets) { si ->
                val k = "$ei-$si"
                val done = WorkoutSession.done[k] == true
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${si + 1}", Modifier.width(34.dp), color = if (done) p.accent else p.subtext, fontWeight = FontWeight.Bold)
                    GlassField(
                        WorkoutSession.weight[k] ?: "", { WorkoutSession.weight[k] = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                        Modifier.weight(1f), placeholder = WorkoutSession.weight["$ei-${si - 1}"] ?: "—", keyboardType = KeyboardType.Decimal, center = true
                    )
                    Spacer(Modifier.width(8.dp))
                    GlassField(
                        WorkoutSession.reps[k] ?: "", { WorkoutSession.reps[k] = it.filter(Char::isDigit).take(3) },
                        Modifier.weight(1f), placeholder = ex.reps.takeWhile { it.isDigit() }.ifEmpty { "—" }, keyboardType = KeyboardType.Number, center = true
                    )
                    Spacer(Modifier.width(10.dp))
                    CheckDot(done, 28.dp) {
                        val nowDone = !done
                        WorkoutSession.done[k] = nowDone
                        if (nowDone) {
                            if (WorkoutSession.weight[k].isNullOrEmpty()) WorkoutSession.weight["$ei-${si - 1}"]?.let { WorkoutSession.weight[k] = it }
                            TimerEngine.startRest(ex.restSec)
                        }
                    }
                }
            }
        }
    }
}
