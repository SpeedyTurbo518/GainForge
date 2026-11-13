package com.gainforge.wear

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

private const val STATE_PATH = "/gainforge/state"

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WatchStore.init(this)
        setContent { WearApp(this) }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
        lifecycleScope.launch {
            runCatching {
                val items = Wearable.getDataClient(this@MainActivity).dataItems.await()
                items.forEach { item ->
                    if (item.uri.path == STATE_PATH) DataMapItem.fromDataItem(item).dataMap.getString("json")?.let(WatchStore::set)
                }
                items.release()
            }
            PhoneLink.send(this@MainActivity, "/gainforge/request", "")
        }
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { e ->
            if (e.type == DataEvent.TYPE_CHANGED && e.dataItem.uri.path == STATE_PATH) {
                DataMapItem.fromDataItem(e.dataItem).dataMap.getString("json")?.let(WatchStore::set)
            }
        }
    }

    fun keepScreenOn(on: Boolean) {
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

// ---------------- Navigation ----------------

private sealed interface WScreen {
    data object Home : WScreen
    data object Workout : WScreen
    data object RestPick : WScreen
    data class Countdown(val seconds: Int, val label: String) : WScreen
    data object IntervalPick : WScreen
    data class Interval(val work: Int, val rest: Int, val rounds: Int) : WScreen
    data object Stopwatch : WScreen
    data object HeartRate : WScreen
}

private val Blue = Color(0xFF0A84FF)
private val Mint = Color(0xFF63E6E2)
private val Glass = Color(0xFF1C1F26)

@Composable
fun WearApp(activity: MainActivity) {
    val payload = WatchStore.payload
    val accent = payload?.let { Color(it.accent) } ?: Blue
    var screen by remember { mutableStateOf<WScreen>(WScreen.Home) }
    val back = { screen = WScreen.Home }

    MaterialTheme(colors = Colors(primary = accent, primaryVariant = accent, secondary = Mint, background = Color.Black, surface = Glass, onPrimary = Color.White, onSurface = Color.White)) {
        BackHandler(enabled = screen != WScreen.Home) { back() }
        when (val s = screen) {
            WScreen.Home -> HomeScreen(payload, accent) { screen = it }
            WScreen.Workout -> WorkoutScreen(activity, payload, accent, back)
            WScreen.RestPick -> PickScreen("Rest timer", listOf(45, 60, 90, 120, 180).map { "${fmtSec(it)} rest" to WScreen.Countdown(it, "Rest") }) { screen = it }
            is WScreen.Countdown -> CountdownScreen(s.seconds, s.label, accent, onDone = back, onCancel = back)
            WScreen.IntervalPick -> PickScreen(
                "Intervals",
                listOf(
                    "Tabata 20/10 ×8" to WScreen.Interval(20, 10, 8),
                    "HIIT 40/20 ×10" to WScreen.Interval(40, 20, 10),
                    "EMOM 60 ×10" to WScreen.Interval(60, 0, 10),
                    "Boxing 3m/1m ×5" to WScreen.Interval(180, 60, 5)
                )
            ) { screen = it }
            is WScreen.Interval -> IntervalScreen(activity, s.work, s.rest, s.rounds, accent, back)
            WScreen.Stopwatch -> StopwatchScreen(accent)
            WScreen.HeartRate -> HeartRateScreen(accent)
        }
    }
}

// ---------------- Home ----------------

@Composable
private fun HomeScreen(p: WatchPayload?, accent: Color, go: (WScreen) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val list = rememberScalingLazyListState()
    Scaffold(timeText = { TimeText() }, vignette = { Vignette(VignettePosition.TopAndBottom) }, positionIndicator = { PositionIndicator(scalingLazyListState = list) }) {
        ScalingLazyColumn(Modifier.fillMaxSize(), state = list, horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Text(
                    if (p != null) "${p.emoji} ${p.name}" else "💪 GainForge",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White
                )
            }
            if (p != null) {
                item { FuelRing(p, accent) }
                item {
                    Chip(
                        onClick = { go(WScreen.Workout) },
                        label = { Text(if (p.doneToday) "Train again" else "Start workout", maxLines = 1) },
                        secondaryLabel = { Text(p.workoutTitle + " · " + p.exercises.size + " ex", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        icon = { Text("🏋️", fontSize = 18.sp) },
                        colors = ChipDefaults.primaryChipColors(backgroundColor = accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Chip(
                        onClick = {
                            WatchStore.addWaterLocal(250)
                            scope.launch { PhoneLink.send(ctx, "/gainforge/water", "250") }
                            buzz(ctx, 60)
                        },
                        label = { Text("Water +250 ml") },
                        secondaryLabel = { Text("${p.waterMl} / ${p.waterTargetMl} ml") },
                        icon = { Text("💧", fontSize = 18.sp) },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                item {
                    Text(
                        "Open GainForge on your phone to sync your plan. Timers work right away.",
                        textAlign = TextAlign.Center, fontSize = 12.sp, color = Color.LightGray, modifier = Modifier.padding(8.dp)
                    )
                }
            }
            item { NavChip("⏱️", "Rest timer", "Between sets") { go(WScreen.RestPick) } }
            item { NavChip("🔥", "Intervals", "Tabata · HIIT · EMOM") { go(WScreen.IntervalPick) } }
            item { NavChip("⏲️", "Stopwatch", null) { go(WScreen.Stopwatch) } }
            item { NavChip("❤️", "Heart rate", null) { go(WScreen.HeartRate) } }
            if (p != null) item {
                val unit = if (p.imperial) "lb" else "kg"
                val f = if (p.imperial) 2.20462 else 1.0
                Text(
                    "Goal ${(p.currentKg * f).roundToInt()} → ${(p.targetKg * f).roundToInt()} $unit",
                    fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun FuelRing(p: WatchPayload, accent: Color) {
    Box(Modifier.size(96.dp).padding(4.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = (p.kcalEaten / p.kcalTarget.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
            modifier = Modifier.fillMaxSize(), indicatorColor = accent, trackColor = Glass, strokeWidth = 7.dp
        )
        CircularProgressIndicator(
            progress = (p.proteinEaten / p.proteinTarget.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
            modifier = Modifier.fillMaxSize().padding(10.dp), indicatorColor = Mint, trackColor = Glass, strokeWidth = 5.dp
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${p.kcalEaten}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("/${p.kcalTarget}", fontSize = 10.sp, color = Color.Gray)
            Text("${p.proteinEaten}g P", fontSize = 10.sp, color = Mint)
        }
    }
}

@Composable
private fun NavChip(emoji: String, label: String, sub: String?, onClick: () -> Unit) {
    Chip(
        onClick = onClick,
        label = { Text(label) },
        secondaryLabel = sub?.let { { Text(it, maxLines = 1) } },
        icon = { Text(emoji, fontSize = 18.sp) },
        colors = ChipDefaults.chipColors(backgroundColor = Glass),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PickScreen(title: String, options: List<Pair<String, WScreen>>, go: (WScreen) -> Unit) {
    val list = rememberScalingLazyListState()
    Scaffold(timeText = { TimeText() }, positionIndicator = { PositionIndicator(scalingLazyListState = list) }) {
        ScalingLazyColumn(Modifier.fillMaxSize(), state = list) {
            item { Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
            options.forEach { (label, target) ->
                item {
                    Chip(onClick = { go(target) }, label = { Text(label) }, colors = ChipDefaults.chipColors(backgroundColor = Glass), modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// ---------------- Workout ----------------

@Composable
private fun WorkoutScreen(activity: MainActivity, p: WatchPayload?, accent: Color, onExit: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val exercises = p?.exercises.orEmpty()
    var exIdx by remember { mutableIntStateOf(0) }
    var setIdx by remember { mutableIntStateOf(0) }
    var setsDone by remember { mutableIntStateOf(0) }
    var resting by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    val startedAt = remember { SystemClock.elapsedRealtime() }
    val hr = rememberHeartRate()

    DisposableEffect(Unit) {
        activity.keepScreenOn(true)
        onDispose { activity.keepScreenOn(false) }
    }

    fun finish() {
        finished = true
        val done = WatchWorkoutDone(p?.workoutTitle ?: "Workout", (SystemClock.elapsedRealtime() - startedAt) / 1000, setsDone)
        WatchStore.markDoneLocal()
        scope.launch { PhoneLink.send(ctx, "/gainforge/workout", WatchStore.json.encodeToString(WatchWorkoutDone.serializer(), done)) }
        buzz(ctx, 500)
    }

    if (exercises.isEmpty()) {
        CenterMessage("No workout synced yet.\nOpen GainForge on your phone.") { onExit() }
        return
    }
    if (finished) {
        CenterMessage("Workout complete! 🎉\n$setsDone sets · ${(SystemClock.elapsedRealtime() - startedAt) / 60000} min\nSaved to your phone.") { onExit() }
        return
    }
    val ex = exercises[exIdx]
    if (resting) {
        CountdownScreen(ex.restSec, "Rest", accent, heartRate = hr.value, onDone = { resting = false }, onCancel = { resting = false })
        return
    }

    Scaffold(timeText = { TimeText() }) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("${exIdx + 1}/${exercises.size} · ${p?.workoutTitle}", fontSize = 11.sp, color = Color.Gray)
            Text(ex.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("Set ${setIdx + 1} of ${ex.sets} · ${ex.reps} reps", fontSize = 12.sp, color = accent)
            hr.value?.let { Text("❤️ $it bpm", fontSize = 11.sp, color = Color(0xFFFF6B6B)) }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CompactButton(onClick = {
                    if (exIdx < exercises.lastIndex) { exIdx++; setIdx = 0 } else finish()
                }, colors = ButtonDefaults.secondaryButtonColors()) { Text("⏭", fontSize = 14.sp) }
                Button(
                    onClick = {
                        setsDone++
                        buzz(ctx, 80)
                        val lastSet = setIdx >= ex.sets - 1
                        if (lastSet && exIdx >= exercises.lastIndex) finish()
                        else {
                            if (lastSet) { exIdx++; setIdx = 0 } else setIdx++
                            resting = true
                        }
                    },
                    colors = ButtonDefaults.primaryButtonColors(backgroundColor = accent),
                    modifier = Modifier.size(ButtonDefaults.LargeButtonSize)
                ) { Text("✓", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                CompactButton(onClick = { finish() }, colors = ButtonDefaults.secondaryButtonColors()) { Text("■", fontSize = 12.sp) }
            }
            Text("Done · Skip · Finish", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun CenterMessage(text: String, onOk: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text, textAlign = TextAlign.Center, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        CompactChip(onClick = onOk, label = { Text("OK") })
    }
}

// ---------------- Timers ----------------

@Composable
private fun CountdownScreen(seconds: Int, label: String, accent: Color, heartRate: Int? = null, onDone: () -> Unit, onCancel: () -> Unit) {
    val ctx = LocalContext.current
    var total by remember { mutableIntStateOf(seconds) }
    var endAt by remember { mutableLongStateOf(SystemClock.elapsedRealtime() + seconds * 1000L) }
    var paused by remember { mutableStateOf<Long?>(null) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val remaining = paused ?: (endAt - now).coerceAtLeast(0)

    LaunchedEffect(endAt, paused) {
        while (paused == null) {
            now = SystemClock.elapsedRealtime()
            val left = endAt - now
            if (left <= 0) { buzzPattern(ctx); onDone(); break }
            delay(100)
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = remaining / (total * 1000f), modifier = Modifier.fillMaxSize().padding(4.dp),
            indicatorColor = accent, trackColor = Glass, strokeWidth = 8.dp
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label.uppercase(), fontSize = 12.sp, color = accent, fontWeight = FontWeight.Bold)
            Text(fmtSec(((remaining + 999) / 1000).toInt()), fontSize = 36.sp, fontWeight = FontWeight.Light)
            heartRate?.let { Text("❤️ $it", fontSize = 11.sp, color = Color(0xFFFF6B6B)) }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CompactChip(onClick = { endAt += 15_000; total += 15; paused = paused?.plus(15_000) }, label = { Text("+15") })
                CompactChip(onClick = {
                    val pz = paused
                    if (pz == null) paused = (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                    else { endAt = SystemClock.elapsedRealtime() + pz; paused = null }
                }, label = { Text(if (paused == null) "❚❚" else "▶") })
                CompactChip(onClick = onCancel, label = { Text("Skip") })
            }
        }
    }
}

@Composable
private fun IntervalScreen(activity: MainActivity, work: Int, rest: Int, rounds: Int, accent: Color, onExit: () -> Unit) {
    val ctx = LocalContext.current
    var round by remember { mutableIntStateOf(1) }
    var working by remember { mutableStateOf(true) }
    var done by remember { mutableStateOf(false) }
    var phaseEnd by remember { mutableLongStateOf(SystemClock.elapsedRealtime() + work * 1000L) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var paused by remember { mutableStateOf<Long?>(null) }
    val hr = rememberHeartRate()

    DisposableEffect(Unit) {
        activity.keepScreenOn(true)
        onDispose { activity.keepScreenOn(false) }
    }

    LaunchedEffect(phaseEnd, paused, done) {
        while (!done && paused == null) {
            now = SystemClock.elapsedRealtime()
            if (now >= phaseEnd) {
                if (working && rest > 0) { working = false; phaseEnd += rest * 1000L; buzz(ctx, 300) }
                else if (round >= rounds) { done = true; buzzPattern(ctx) }
                else { round++; working = true; phaseEnd += work * 1000L; buzz(ctx, 300) }
                break
            }
            delay(100)
        }
    }

    val phaseLen = (if (working) work else rest) * 1000f
    val remaining = paused ?: (phaseEnd - now).coerceAtLeast(0)
    val color = if (working) accent else Mint

    if (done) { CenterMessage("Intervals done! 🔥\n$rounds rounds") { onExit() }; return }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(progress = remaining / phaseLen, modifier = Modifier.fillMaxSize().padding(4.dp), indicatorColor = color, trackColor = Glass, strokeWidth = 8.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (working) "WORK" else "REST", color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(fmtSec(((remaining + 999) / 1000).toInt()), fontSize = 36.sp, fontWeight = FontWeight.Light)
            Text("Round $round/$rounds" + (hr.value?.let { " · ❤️$it" } ?: ""), fontSize = 11.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CompactChip(onClick = {
                    val pz = paused
                    if (pz == null) paused = (phaseEnd - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                    else { phaseEnd = SystemClock.elapsedRealtime() + pz; paused = null }
                }, label = { Text(if (paused == null) "❚❚" else "▶") })
                CompactChip(onClick = onExit, label = { Text("End") })
            }
        }
    }
}

@Composable
private fun StopwatchScreen(accent: Color) {
    var running by remember { mutableStateOf(false) }
    var start by remember { mutableLongStateOf(0L) }
    var accum by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(running) { while (running) { now = SystemClock.elapsedRealtime(); delay(50) } }
    val elapsed = accum + if (running) now - start else 0
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(progress = (elapsed % 60_000) / 60_000f, modifier = Modifier.fillMaxSize().padding(4.dp), indicatorColor = accent, trackColor = Glass, strokeWidth = 6.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(fmtSec((elapsed / 1000).toInt()) + ".%d".format((elapsed % 1000) / 100), fontSize = 32.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactChip(onClick = { running = false; accum = 0 }, label = { Text("Reset") })
                CompactChip(onClick = {
                    if (running) { accum += SystemClock.elapsedRealtime() - start; running = false }
                    else { start = SystemClock.elapsedRealtime(); now = start; running = true }
                }, label = { Text(if (running) "Stop" else "Start") }, colors = ChipDefaults.primaryChipColors(backgroundColor = accent))
            }
        }
    }
}

@Composable
private fun HeartRateScreen(accent: Color) {
    val hr = rememberHeartRate()
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(70.dp).clip(CircleShape).background(Color(0x33FF453A)), contentAlignment = Alignment.Center) { Text("❤️", fontSize = 30.sp) }
        Spacer(Modifier.height(8.dp))
        Text(hr.value?.toString() ?: "--", fontSize = 40.sp, fontWeight = FontWeight.Light)
        Text(if (hr.value == null) "Hold still… (allow sensor access)" else "bpm", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
        Text("Keep the watch snug on your wrist", fontSize = 10.sp, color = accent, modifier = Modifier.padding(top = 4.dp))
    }
}

// ---------------- Sensors & haptics ----------------

private val hrPermission = if (Build.VERSION.SDK_INT >= 36) "android.permission.health.READ_HEART_RATE" else Manifest.permission.BODY_SENSORS

@Composable
private fun rememberHeartRate(): State<Int?> {
    val ctx = LocalContext.current
    val hr = remember { mutableStateOf<Int?>(null) }
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, hrPermission) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) runCatching { launcher.launch(hrPermission) } }
    DisposableEffect(granted) {
        val sm = ctx.getSystemService(SensorManager::class.java)
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) { val v = e.values.firstOrNull()?.roundToInt() ?: 0; if (v > 0) hr.value = v }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (granted && sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sm?.unregisterListener(listener) }
    }
    return hr
}

private fun vibrator(ctx: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
    else @Suppress("DEPRECATION") ctx.getSystemService(Vibrator::class.java)

private fun buzz(ctx: Context, ms: Long) {
    runCatching { vibrator(ctx)?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)) }
}

private fun buzzPattern(ctx: Context) {
    runCatching { vibrator(ctx)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 350, 150, 350, 150, 500), -1)) }
}

private fun fmtSec(s: Int): String = "%d:%02d".format(s / 60, s % 60)
