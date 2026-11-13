package com.gainforge.wear

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Field names must match com.gainforge.app.data.WatchModels in the phone app.

@Serializable
data class WatchExercise(val name: String, val sets: Int, val reps: String, val restSec: Int)

@Serializable
data class WatchPayload(
    val name: String,
    val emoji: String,
    val accent: Long,
    val kcalTarget: Int,
    val kcalEaten: Int,
    val proteinTarget: Int,
    val proteinEaten: Int,
    val waterTargetMl: Int,
    val waterMl: Int,
    val currentKg: Double,
    val targetKg: Double,
    val imperial: Boolean,
    val workoutTitle: String,
    val workoutFocus: String,
    val exercises: List<WatchExercise>,
    val doneToday: Boolean
)

@Serializable
data class WatchWorkoutDone(val title: String, val durationSec: Long, val sets: Int)

/** Last plan received from the phone, cached so the watch works when the phone is away. */
object WatchStore {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    var payload by mutableStateOf<WatchPayload?>(null)
        private set
    private var ctx: Context? = null

    fun init(context: Context) {
        ctx = context.applicationContext
        val raw = context.getSharedPreferences("gf", Context.MODE_PRIVATE).getString("payload", null)
        if (raw != null && payload == null) payload = runCatching { json.decodeFromString(WatchPayload.serializer(), raw) }.getOrNull()
    }

    fun set(raw: String) {
        if (raw.isBlank()) { // profile was reset on the phone
            payload = null
            ctx?.getSharedPreferences("gf", Context.MODE_PRIVATE)?.edit()?.remove("payload")?.apply()
            return
        }
        val p = runCatching { json.decodeFromString(WatchPayload.serializer(), raw) }.getOrNull() ?: return
        payload = p
        ctx?.getSharedPreferences("gf", Context.MODE_PRIVATE)?.edit()?.putString("payload", raw)?.apply()
    }

    /** Optimistic local update so the UI responds instantly; the phone confirms with a fresh sync. */
    fun addWaterLocal(ml: Int) { payload = payload?.let { it.copy(waterMl = it.waterMl + ml) } }
    fun markDoneLocal() { payload = payload?.copy(doneToday = true) }
}

object PhoneLink {
    suspend fun send(context: Context, path: String, body: String): Boolean = runCatching {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        nodes.forEach { Wearable.getMessageClient(context).sendMessage(it.id, path, body.toByteArray()).await() }
        nodes.isNotEmpty()
    }.getOrDefault(false)
}
