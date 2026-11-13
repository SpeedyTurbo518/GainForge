package com.gainforge.app.logic

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.*
import com.gainforge.app.data.WorkoutLog
import com.gainforge.app.data.Units
import com.gainforge.app.data.today
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

enum class IntervalPhase(val label: String) { IDLE("Ready"), PREP("Get ready"), WORK("Work"), REST("Rest"), DONE("Done") }

/** App-wide timers. Lives as long as the app process, so timers keep running when you switch tabs. */
object TimerEngine {
    private fun clock() = SystemClock.elapsedRealtime()
    private val scope = MainScope()
    private var job: Job? = null
    private var ctx: Context? = null
    private var tone: ToneGenerator? = null

    var now by mutableLongStateOf(clock()); private set

    fun init(context: Context) {
        ctx = context.applicationContext
    }

    // ---- Stopwatch ----
    var swRunning by mutableStateOf(false); private set
    private var swStart by mutableLongStateOf(0L)
    private var swAccum by mutableLongStateOf(0L)
    val laps = mutableStateListOf<Long>()
    fun swElapsed(): Long = swAccum + if (swRunning) max(0, now - swStart) else 0
    fun swToggle() {
        if (swRunning) { swAccum += clock() - swStart; swRunning = false }
        else { swStart = clock(); now = swStart; swRunning = true; ensure() }
    }
    fun swLap() { laps.add(0, swElapsed()) }
    fun swReset() { swRunning = false; swAccum = 0; laps.clear() }

    // ---- Countdown ----
    var cdTotal by mutableLongStateOf(60_000L); private set
    private var cdEnd by mutableLongStateOf(0L)
    private var cdPaused by mutableLongStateOf(60_000L)
    var cdRunning by mutableStateOf(false); private set
    fun cdRemaining(): Long = if (cdRunning) max(0, cdEnd - now) else cdPaused
    fun cdSet(ms: Long) { cdTotal = ms.coerceAtLeast(5_000); cdPaused = cdTotal; cdRunning = false }
    fun cdToggle() {
        if (cdRunning) { cdPaused = cdRemaining(); cdRunning = false }
        else {
            if (cdPaused <= 0) cdPaused = cdTotal
            cdEnd = clock() + cdPaused; now = clock(); cdRunning = true; ensure()
        }
    }
    fun cdReset() { cdRunning = false; cdPaused = cdTotal }

    // ---- Intervals (HIIT / Tabata / EMOM) ----
    var ivWork by mutableIntStateOf(40)
    var ivRest by mutableIntStateOf(20)
    var ivRounds by mutableIntStateOf(10)
    var ivRound by mutableIntStateOf(0); private set
    var ivPhase by mutableStateOf(IntervalPhase.IDLE); private set
    private var ivEnd by mutableLongStateOf(0L)
    private var ivPaused by mutableLongStateOf(0L)
    var ivRunning by mutableStateOf(false); private set
    private var lastBeepSec = -1L
    fun ivPhaseTotal(): Long = when (ivPhase) {
        IntervalPhase.PREP -> 5_000L
        IntervalPhase.WORK -> ivWork * 1000L
        IntervalPhase.REST -> ivRest * 1000L
        else -> 1L
    }
    fun ivRemaining(): Long = when {
        ivRunning -> max(0, ivEnd - now)
        ivPhase == IntervalPhase.IDLE || ivPhase == IntervalPhase.DONE -> 0
        else -> ivPaused
    }
    fun ivStart() {
        ivRound = 1; ivPhase = IntervalPhase.PREP; ivEnd = clock() + 5_000; now = clock(); ivRunning = true; ensure()
    }
    fun ivToggle() {
        if (ivPhase == IntervalPhase.IDLE || ivPhase == IntervalPhase.DONE) { ivStart(); return }
        if (ivRunning) { ivPaused = ivRemaining(); ivRunning = false }
        else { ivEnd = clock() + ivPaused; now = clock(); ivRunning = true; ensure() }
    }
    fun ivReset() { ivRunning = false; ivPhase = IntervalPhase.IDLE; ivRound = 0 }

    // ---- Rest timer between sets ----
    var restEnd by mutableLongStateOf(0L); private set
    var restTotal by mutableLongStateOf(0L); private set
    fun restRemaining(): Long = max(0, restEnd - now)
    fun restActive(): Boolean = restEnd != 0L && restEnd > now
    fun startRest(sec: Int) { restTotal = sec * 1000L; restEnd = clock() + restTotal; now = clock(); ensure() }
    fun addRest(sec: Int) { if (restEnd != 0L) { restEnd += sec * 1000L; restTotal += sec * 1000L } }
    fun skipRest() { restEnd = 0L }

    // ---- Ticker ----
    fun ensure() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (anyActive()) {
                now = clock()
                transitions()
                delay(100)
            }
            now = clock()
        }
    }

    private fun anyActive() = swRunning || cdRunning || ivRunning || restEnd != 0L || WorkoutSession.active

    private fun transitions() {
        if (cdRunning && now >= cdEnd) { cdRunning = false; cdPaused = 0; alarm(true) }
        if (restEnd != 0L && now >= restEnd) { restEnd = 0L; alarm(true) }
        if (ivRunning) {
            val remainSec = (ivEnd - now + 999) / 1000
            if (remainSec in 1..3 && remainSec != lastBeepSec) { lastBeepSec = remainSec; beep(false) }
            if (now >= ivEnd) {
                lastBeepSec = -1
                when (ivPhase) {
                    IntervalPhase.PREP -> goPhase(IntervalPhase.WORK, ivWork)
                    IntervalPhase.WORK -> if (ivRest > 0) goPhase(IntervalPhase.REST, ivRest) else nextRound()
                    IntervalPhase.REST -> nextRound()
                    else -> ivRunning = false
                }
            }
        }
    }

    private fun nextRound() {
        if (ivRound >= ivRounds) { ivPhase = IntervalPhase.DONE; ivRunning = false; alarm(true) }
        else { ivRound += 1; goPhase(IntervalPhase.WORK, ivWork) }
    }

    private fun goPhase(phase: IntervalPhase, sec: Int) {
        ivPhase = phase; ivEnd += sec * 1000L; beep(true)
    }

    private fun vibrator(): Vibrator? {
        val c = ctx ?: return null
        return if (Build.VERSION.SDK_INT >= 31) c.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") c.getSystemService(Vibrator::class.java)
    }

    private fun toneGen(): ToneGenerator? {
        if (tone == null) tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 85) }.getOrNull()
        return tone
    }

    private fun beep(strong: Boolean) {
        runCatching { toneGen()?.startTone(if (strong) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP, if (strong) 250 else 120) }
        runCatching { vibrator()?.vibrate(VibrationEffect.createOneShot(if (strong) 300 else 80, VibrationEffect.DEFAULT_AMPLITUDE)) }
    }

    fun alarm(long: Boolean) {
        runCatching { toneGen()?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, if (long) 900 else 300) }
        runCatching {
            val pattern = if (long) longArrayOf(0, 400, 150, 400, 150, 600) else longArrayOf(0, 250)
            vibrator()?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }
}

/** The workout currently being logged on the Train tab. */
object WorkoutSession {
    var active by mutableStateOf(false); private set
    var startedAt by mutableLongStateOf(0L); private set
    var dayIndex by mutableIntStateOf(0); private set
    val done = mutableStateMapOf<String, Boolean>()
    val weight = mutableStateMapOf<String, String>()
    val reps = mutableStateMapOf<String, String>()

    fun start(index: Int) {
        done.clear(); weight.clear(); reps.clear()
        dayIndex = index; startedAt = SystemClock.elapsedRealtime(); active = true
        TimerEngine.ensure()
    }

    fun cancel() { active = false; done.clear(); weight.clear(); reps.clear(); TimerEngine.skipRest() }

    fun buildLog(day: WorkoutDay, imperial: Boolean): WorkoutLog {
        var volume = 0.0
        done.filterValues { it }.keys.forEach { k ->
            val w = weight[k]?.toDoubleOrNull() ?: 0.0
            val parts = k.split("-")
            val ex = day.exercises.getOrNull(parts[0].toInt())
            val r = reps[k]?.toIntOrNull() ?: ex?.reps?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
            volume += Units.toKg(w, imperial) * r
        }
        val dur = (SystemClock.elapsedRealtime() - startedAt) / 1000
        return WorkoutLog(today(), day.title, dur, done.count { it.value }, volume)
    }
}

fun fmtClock(ms: Long, hundredths: Boolean = false): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    val base = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    return if (hundredths) base + ".%02d".format((ms % 1000) / 10) else base
}

fun fmtCountdown(ms: Long): String = fmtClock(((ms + 999) / 1000) * 1000)
