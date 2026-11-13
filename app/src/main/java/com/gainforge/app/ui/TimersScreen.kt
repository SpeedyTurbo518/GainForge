package com.gainforge.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gainforge.app.logic.IntervalPhase
import com.gainforge.app.logic.TimerEngine
import com.gainforge.app.logic.fmtClock
import com.gainforge.app.logic.fmtCountdown

@Composable
fun TimersScreen() {
    var seg by rememberSaveable { mutableIntStateOf(0) }
    ScreenColumn {
        item { LargeTitle("Timers") }
        item { Segmented(listOf("Stopwatch", "Countdown", "Intervals"), seg) { seg = it } }
        item {
            when (seg) {
                0 -> StopwatchPanel()
                1 -> CountdownPanel()
                else -> IntervalPanel()
            }
        }
        item { Sub("Timers keep running while you switch tabs. You'll get a beep and a buzz when time's up.", Modifier.padding(horizontal = 4.dp)) }
    }
}

@Composable
private fun BigTime(text: String, label: String, labelColor: Color? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, fontSize = 52.sp, fontWeight = FontWeight.Light, color = LocalPalette.current.text, style = Tnum, letterSpacing = (-1).sp)
        Caption(label, color = labelColor)
    }
}

@Composable
private fun StopwatchPanel() {
    val p = LocalPalette.current
    val t = TimerEngine
    val e = t.swElapsed()
    GlassCard {
        Ring((e % 60_000) / 60_000f, Modifier.size(270.dp).align(Alignment.CenterHorizontally), stroke = 10.dp, animate = false) {
            BigTime(fmtClock(e, hundredths = true), "Stopwatch")
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            RoundButton(if (t.swRunning) "Lap" else "Reset", null) { if (t.swRunning) t.swLap() else t.swReset() }
            RoundButton(if (t.swRunning) "Stop" else "Start", if (t.swRunning) p.danger else p.good) { t.swToggle() }
        }
        if (t.laps.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            t.laps.forEachIndexed { i, lap ->
                val prev = t.laps.getOrNull(i + 1) ?: 0L
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Body("Lap ${t.laps.size - i}", Modifier.weight(1f))
                    Text("+" + fmtClock(lap - prev, true), color = p.subtext, style = Tnum, modifier = Modifier.padding(end = 14.dp))
                    Text(fmtClock(lap, true), color = p.text, style = Tnum)
                }
            }
        }
    }
}

@Composable
private fun CountdownPanel() {
    val p = LocalPalette.current
    val t = TimerEngine
    val rem = t.cdRemaining()
    val finished = !t.cdRunning && rem == 0L
    GlassCard {
        Ring(if (t.cdTotal > 0) rem / t.cdTotal.toFloat() else 0f, Modifier.size(270.dp).align(Alignment.CenterHorizontally), stroke = 10.dp, animate = false) {
            BigTime(fmtCountdown(rem), if (finished) "Time's up!" else "Countdown", if (finished) p.accent else null)
        }
        Spacer(Modifier.height(14.dp))
        if (!t.cdRunning) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 60, 90, 120, 180, 300, 600).forEach { s ->
                    SelectChip(if (s < 60) "${s}s" else if (s % 60 == 0) "${s / 60}m" else "${s / 60}:${"%02d".format(s % 60)}", t.cdTotal == s * 1000L) { t.cdSet(s * 1000L) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("−15s", Modifier.weight(1f)) { t.cdSet(t.cdTotal - 15_000) }
                SecondaryButton("+15s", Modifier.weight(1f)) { t.cdSet(t.cdTotal + 15_000) }
            }
            Spacer(Modifier.height(12.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            RoundButton("Reset", null) { t.cdReset() }
            RoundButton(
                when { t.cdRunning -> "Pause"; rem in 1 until t.cdTotal -> "Resume"; else -> "Start" },
                if (t.cdRunning) p.warn else p.good
            ) { t.cdToggle() }
        }
    }
}

@Composable
private fun IntervalPanel() {
    val p = LocalPalette.current
    val t = TimerEngine
    val idle = t.ivPhase == IntervalPhase.IDLE
    val phaseColor = when (t.ivPhase) {
        IntervalPhase.WORK -> p.accent
        IntervalPhase.REST -> p.mint
        IntervalPhase.PREP -> p.warn
        else -> p.good
    }
    GlassCard {
        if (idle) {
            Caption("Presets")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple("Tabata", 20, 10) to 8,
                    Triple("HIIT", 40, 20) to 10,
                    Triple("EMOM", 60, 0) to 10,
                    Triple("Boxing", 180, 60) to 5,
                    Triple("Circuit", 45, 15) to 12
                ).forEach { (cfg, rounds) ->
                    val sel = t.ivWork == cfg.second && t.ivRest == cfg.third && t.ivRounds == rounds
                    SelectChip(cfg.first, sel) { t.ivWork = cfg.second; t.ivRest = cfg.third; t.ivRounds = rounds }
                }
            }
            Spacer(Modifier.height(12.dp))
            Stepper("Work", "${t.ivWork}s", { t.ivWork = (t.ivWork - 5).coerceAtLeast(5) }, { t.ivWork = (t.ivWork + 5).coerceAtMost(600) })
            Stepper("Rest", "${t.ivRest}s", { t.ivRest = (t.ivRest - 5).coerceAtLeast(0) }, { t.ivRest = (t.ivRest + 5).coerceAtMost(600) })
            Stepper("Rounds", "${t.ivRounds}", { t.ivRounds = (t.ivRounds - 1).coerceAtLeast(1) }, { t.ivRounds = (t.ivRounds + 1).coerceAtMost(50) })
            Spacer(Modifier.height(10.dp))
            Sub("Total ${fmtClock((t.ivWork + t.ivRest) * t.ivRounds * 1000L)}", Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Start intervals", Modifier.fillMaxWidth()) { t.ivStart() }
        } else {
            val rem = t.ivRemaining()
            Ring(
                if (t.ivPhase == IntervalPhase.DONE) 1f else rem / t.ivPhaseTotal().toFloat(),
                Modifier.size(270.dp).align(Alignment.CenterHorizontally), stroke = 12.dp, color = phaseColor, animate = false
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(t.ivPhase.label.uppercase(), color = phaseColor, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 2.sp)
                    Text(if (t.ivPhase == IntervalPhase.DONE) "🎉" else fmtCountdown(rem), fontSize = 56.sp, fontWeight = FontWeight.Light, color = p.text, style = Tnum)
                    Sub("Round ${t.ivRound.coerceAtLeast(1)} of ${t.ivRounds}")
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                RoundButton("Reset", null) { t.ivReset() }
                if (t.ivPhase != IntervalPhase.DONE)
                    RoundButton(if (t.ivRunning) "Pause" else "Resume", if (t.ivRunning) p.warn else p.good) { t.ivToggle() }
                else RoundButton("Again", p.good) { t.ivStart() }
            }
        }
    }
}
