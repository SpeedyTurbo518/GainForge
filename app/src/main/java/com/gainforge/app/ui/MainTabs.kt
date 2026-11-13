package com.gainforge.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gainforge.app.data.AppState
import com.gainforge.app.data.Profile
import com.gainforge.app.logic.GoalEngine
import com.gainforge.app.logic.Programs
import com.gainforge.app.logic.TimerEngine
import com.gainforge.app.logic.fmtCountdown

enum class AppTab(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Rounded.Home),
    TRAIN("Train", Icons.Rounded.FitnessCenter),
    TIMERS("Timers", Icons.Rounded.Timer),
    MEALS("Meals", Icons.Rounded.Restaurant),
    GOALS("Goals", Icons.Rounded.Flag),
    PROFILE("Profile", Icons.Rounded.Person)
}

@Composable
fun MainTabs(profile: Profile, onEditAnswers: () -> Unit, onAddProfile: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(AppTab.TODAY) }
    val imperial = AppState.data.imperial
    val plan = remember(profile) { GoalEngine.compute(profile) }
    val program = remember(profile) { Programs.build(profile) }

    AppBackground {
        Crossfade(tab, label = "tabs") { t ->
            when (t) {
                AppTab.TODAY -> TodayScreen(profile, plan, program, imperial) { tab = it }
                AppTab.TRAIN -> TrainScreen(profile, program, imperial)
                AppTab.TIMERS -> TimersScreen()
                AppTab.MEALS -> MealsScreen(profile, plan)
                AppTab.GOALS -> GoalsScreen(profile, plan, imperial)
                AppTab.PROFILE -> ProfileScreen(profile, plan, onEditAnswers, onAddProfile)
            }
        }
        val pal = LocalPalette.current
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(pal.bg, pal.bg.copy(alpha = .75f))))
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            RestBanner()
            TabBar(tab) { tab = it }
        }
    }
}

@Composable
private fun TabBar(current: AppTab, onSelect: (AppTab) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(30.dp), solid = true).padding(6.dp)) {
        AppTab.entries.forEach { t ->
            val sel = t == current
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (sel) p.accent.copy(alpha = .15f) else Color.Transparent)
                    .clickable { onSelect(t) }
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(t.icon, t.label, tint = if (sel) p.accent else p.subtext, modifier = Modifier.size(23.dp))
                Text(t.label, fontSize = 10.sp, color = if (sel) p.accent else p.subtext, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

/** Floating rest-timer pill shown on every tab while resting between sets. */
@Composable
private fun RestBanner() {
    val p = LocalPalette.current
    val active = TimerEngine.restActive()
    AnimatedVisibility(active, enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }) {
        val remaining = TimerEngine.restRemaining()
        Row(
            Modifier
                .padding(bottom = 8.dp)
                .fillMaxWidth()
                .glass(CircleShape, highlighted = true, solid = true)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Ring(
                if (TimerEngine.restTotal > 0) remaining / TimerEngine.restTotal.toFloat() else 0f,
                Modifier.size(38.dp), stroke = 4.dp, animate = false
            ) { Icon(Icons.Rounded.Timer, null, tint = p.accent, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Caption("Rest")
                Text(fmtCountdown(remaining), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = p.text, style = Tnum)
            }
            SelectChip("+15s", false) { TimerEngine.addRest(15) }
            Spacer(Modifier.width(6.dp))
            SelectChip("Skip", true) { TimerEngine.skipRest() }
        }
    }
}
