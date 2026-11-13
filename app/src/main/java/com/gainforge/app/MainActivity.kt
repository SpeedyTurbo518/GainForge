package com.gainforge.app

import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.gainforge.app.data.*
import com.gainforge.app.logic.TimerEngine
import com.gainforge.app.ui.GlassTheme
import com.gainforge.app.ui.MainTabs
import com.gainforge.app.ui.Onboarding
import com.gainforge.app.wear.WatchSync

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppState.init(this)
        TimerEngine.init(this)
        WatchSync.push(this, AppState.data)
        setContent { GainForgeApp(this) }
    }
}

private enum class Screen { MAIN, ADD_PROFILE, EDIT_PROFILE }

@Composable
fun GainForgeApp(activity: ComponentActivity) {
    val data = AppState.data
    val dark = when (data.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    LaunchedEffect(dark) {
        val style = if (dark) SystemBarStyle.dark(AColor.TRANSPARENT) else SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT)
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
    val active = AppState.active
    var screen by remember { mutableStateOf(Screen.MAIN) }

    GlassTheme(dark, Color(active?.accent ?: 0xFF0A84FF)) {
        when {
            active == null -> Onboarding(initial = null, firstProfile = true, onCancel = null, onDone = { p ->
                AppState.update { it.copy(profiles = it.profiles + p, activeId = p.id) }
            })
            screen == Screen.ADD_PROFILE -> Onboarding(initial = null, firstProfile = false, onCancel = { screen = Screen.MAIN }, onDone = { p ->
                AppState.update { it.copy(profiles = it.profiles + p, activeId = p.id) }
                screen = Screen.MAIN
            })
            screen == Screen.EDIT_PROFILE -> Onboarding(initial = active, firstProfile = false, onCancel = { screen = Screen.MAIN }, onDone = { p ->
                AppState.update { d -> d.copy(profiles = d.profiles.map { if (it.id == p.id) p else it }) }
                screen = Screen.MAIN
            })
            else -> MainTabs(active, onEditAnswers = { screen = Screen.EDIT_PROFILE }, onAddProfile = { screen = Screen.ADD_PROFILE })
        }
    }
}
