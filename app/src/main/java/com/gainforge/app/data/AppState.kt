package com.gainforge.app.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gainforge.app.wear.WatchSync
import kotlinx.serialization.json.Json
import java.io.File

/** Single source of truth. Everything is stored offline in one JSON file in app storage. */
object AppState {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    var data by mutableStateOf(AppData())
        private set

    private var file: File? = null
    private var appContext: Context? = null
    private val main = Handler(Looper.getMainLooper())

    fun init(context: Context) {
        if (file != null) return
        appContext = context.applicationContext
        val f = File(context.filesDir, "gainforge.json")
        file = f
        data = runCatching { json.decodeFromString(AppData.serializer(), f.readText()) }.getOrElse { AppData() }
    }

    val active: Profile?
        get() = data.profiles.firstOrNull { it.id == data.activeId } ?: data.profiles.firstOrNull()

    fun update(block: (AppData) -> AppData) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post { update(block) }
            return
        }
        data = block(data)
        save()
        appContext?.let { WatchSync.push(it, data) }
    }

    fun updateActive(block: (Profile) -> Profile) = update { d ->
        val id = d.activeId ?: d.profiles.firstOrNull()?.id
        d.copy(profiles = d.profiles.map { if (it.id == id) block(it) else it })
    }

    private fun save() {
        val f = file ?: return
        runCatching {
            val tmp = File(f.parentFile, "gainforge.tmp")
            tmp.writeText(json.encodeToString(AppData.serializer(), data))
            if (!tmp.renameTo(f)) { f.writeText(tmp.readText()); tmp.delete() }
        }
    }
}
