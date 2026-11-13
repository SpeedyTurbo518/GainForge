package com.gainforge.app.wear

import android.content.Context
import com.gainforge.app.data.*
import com.gainforge.app.logic.GoalEngine
import com.gainforge.app.logic.Programs
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.tasks.await

/**
 * Phone → Galaxy Watch sync over Bluetooth using the Wearable Data Layer.
 * No internet needed. Requires the Galaxy Wearable app to be paired with the watch.
 */
object WatchSync {
    const val STATE_PATH = "/gainforge/state"

    fun payload(data: AppData): WatchPayload? {
        val p = data.profiles.firstOrNull { it.id == data.activeId } ?: data.profiles.firstOrNull() ?: return null
        val plan = GoalEngine.compute(p)
        val day = Programs.today(p)
        val intake = p.todayIntake()
        return WatchPayload(
            name = p.name, emoji = p.emoji, accent = p.accent,
            kcalTarget = plan.calories, kcalEaten = intake.kcal,
            proteinTarget = plan.protein, proteinEaten = intake.protein,
            waterTargetMl = plan.waterMl, waterMl = intake.waterMl,
            currentKg = p.health.weightKg, targetKg = plan.targetKg, imperial = data.imperial,
            workoutTitle = day.title, workoutFocus = day.focus,
            exercises = day.exercises.map { WatchExercise(it.name, it.sets, it.reps, it.restSec) },
            doneToday = p.workouts.any { it.date == today() }
        )
    }

    fun push(context: Context, data: AppData) {
        // An empty string tells the watch there's no profile any more, so it clears its cached plan.
        val json = payload(data)?.let { AppState.json.encodeToString(WatchPayload.serializer(), it) } ?: ""
        runCatching {
            val req = PutDataMapRequest.create(STATE_PATH).apply {
                dataMap.putString("json", json)
                dataMap.putLong("ts", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context).putDataItem(req)
        }
    }

    suspend fun status(context: Context): String = runCatching {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        if (nodes.isEmpty()) "No watch connected" else "Connected to " + nodes.joinToString { it.displayName }
    }.getOrElse { "Watch services unavailable on this phone" }
}

/** Receives quick logs from the watch: water, finished workouts, and sync requests. */
class PhoneListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        AppState.init(applicationContext)
        val body = String(event.data)
        when (event.path) {
            "/gainforge/water" -> AppState.updateActive { it.addWater(body.toIntOrNull() ?: 250) }
            "/gainforge/workout" -> runCatching {
                AppState.json.decodeFromString(WatchWorkoutDone.serializer(), body)
            }.onSuccess { w ->
                AppState.updateActive { p ->
                    p.copy(
                        workouts = p.workouts + WorkoutLog(today(), "${w.title} (watch)", w.durationSec, w.sets, 0.0),
                        programDay = p.programDay + 1
                    )
                }
            }
            "/gainforge/request" -> WatchSync.push(applicationContext, AppState.data)
        }
    }
}
