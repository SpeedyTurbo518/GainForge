package com.gainforge.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gainforge.app.data.*
import com.gainforge.app.logic.Plan
import com.gainforge.app.logic.WorkoutSession
import androidx.compose.ui.text.style.TextAlign
import com.gainforge.app.wear.WatchSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(profile: Profile, plan: Plan, onEditAnswers: () -> Unit, onAddProfile: () -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val data = AppState.data
    var name by remember(profile.id) { mutableStateOf(profile.name) }
    var deleteTarget by remember { mutableStateOf<Profile?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    var watchStatus by remember { mutableStateOf("Checking…") }
    LaunchedEffect(Unit) { watchStatus = WatchSync.status(ctx) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { savePhoto(ctx, uri, profile.id) }
            if (path != null) {
                profile.photoPath?.let { old -> runCatching { File(old).delete() } }
                AppState.updateActive { it.copy(photoPath = path) }
            }
        }
    }

    ScreenColumn {
        item { LargeTitle("Profile") }

        item {
            GlassCard {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(profile, 112.dp)
                    Spacer(Modifier.height(10.dp))
                    Title(profile.name, size = 24.sp)
                    Sub("${profile.goal.physique.emoji} ${profile.goal.physique.title} · ${profile.health.days} days/week · ${Units.weight(profile.health.weightKg, data.imperial)} → ${Units.weight(plan.targetKg, data.imperial)}")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton(if (profile.photoPath == null) "Add photo" else "Change photo", icon = Icons.Rounded.PhotoCamera) {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        if (profile.photoPath != null) SecondaryButton("Remove", color = p.danger) {
                            runCatching { File(profile.photoPath).delete() }
                            AppState.updateActive { it.copy(photoPath = null) }
                        }
                    }
                }
            }
        }

        item {
            GlassCard {
                Caption("Name")
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassField(name, { name = it.take(24) }, Modifier.weight(1f))
                    if (name.isNotBlank() && name != profile.name) {
                        Spacer(Modifier.width(8.dp))
                        SelectChip("Save", true) { AppState.updateActive { it.copy(name = name.trim()) } }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Caption("Avatar emoji")
                Spacer(Modifier.height(8.dp))
                ChipGroup(Options.emojis, { it == profile.emoji }) { e -> AppState.updateActive { it.copy(emoji = e) } }
                Spacer(Modifier.height(14.dp))
                Caption("Accent color")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Options.accents.forEach { c ->
                        val sel = c == profile.accent
                        Box(
                            Modifier.size(38.dp).clip(CircleShape).background(Color(c))
                                .border(if (sel) 3.dp else 1.dp, if (sel) p.text else Color.White.copy(alpha = .5f), CircleShape)
                                .clickable { AppState.updateActive { it.copy(accent = c) } },
                            contentAlignment = Alignment.Center
                        ) { if (sel) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }

        item {
            GlassCard {
                Caption("Appearance")
                Spacer(Modifier.height(8.dp))
                Segmented(ThemeMode.entries.map { it.label }, data.theme.ordinal) { i -> AppState.update { it.copy(theme = ThemeMode.entries[i]) } }
                Spacer(Modifier.height(14.dp))
                Caption("Units")
                Spacer(Modifier.height(8.dp))
                Segmented(listOf("Imperial (lb, ft)", "Metric (kg, cm)"), if (data.imperial) 0 else 1) { i -> AppState.update { it.copy(imperial = i == 0) } }
            }
        }

        item {
            GlassCard {
                Caption("Health & food answers")
                Spacer(Modifier.height(6.dp))
                Sub("${profile.health.sex.label}, ${profile.health.age} · ${Units.height(profile.health.heightCm, data.imperial)} · ${profile.health.experience.label} · ${profile.health.equipment.label}")
                Sub("Diet: ${profile.food.diet.label}${if (profile.food.allergies.isNotEmpty()) " · avoids " + profile.food.allergies.joinToString { it.label.lowercase() } else ""}")
                if (profile.health.conditions.isNotEmpty()) Sub("Health: " + profile.health.conditions.joinToString { it.label })
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Edit questionnaire", Modifier.fillMaxWidth(), Icons.Rounded.Edit, onClick = onEditAnswers)
            }
        }

        item { Caption("Profiles · you & friends", Modifier.padding(start = 4.dp, top = 4.dp)) }
        items(data.profiles, key = { it.id }) { pr ->
            val active = pr.id == profile.id
            Row(
                Modifier.fillMaxWidth().glass(highlighted = active)
                    .clickable(enabled = !active) { AppState.update { it.copy(activeId = pr.id) } }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(pr, 46.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(pr.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = p.text)
                    Sub(if (active) "Active profile" else if (pr.isFriend) "Friend · tap to switch" else "Tap to switch")
                }
                if (active) Tag("Active")
                else IconButton(onClick = { deleteTarget = pr }) { Icon(Icons.Rounded.DeleteOutline, "Delete", tint = p.subtext) }
            }
        }
        item { SecondaryButton("Add a friend", Modifier.fillMaxWidth(), Icons.Rounded.PersonAdd, onClick = onAddProfile) }

        item {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Watch, null, tint = p.accent, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Title("Galaxy Watch")
                        Sub(watchStatus)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Sub("Install GainForge on your Galaxy Watch (Wear OS). Today's workout, rest timer, intervals, calories and water sync over Bluetooth — no internet needed. Workouts and water logged on the watch come back here.")
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Sync to watch now", Modifier.fillMaxWidth(), Icons.Rounded.Sync) {
                    WatchSync.push(ctx, AppState.data)
                    scope.launch { watchStatus = WatchSync.status(ctx) }
                }
            }
        }

        item {
            GlassCard {
                Caption("Danger zone", color = p.danger)
                Spacer(Modifier.height(6.dp))
                Sub("Erase ${profile.name}'s entire profile: questionnaire answers, goals, workout history, weight log, meals, favorites and photo. This can't be undone.")
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Reset profile", Modifier.fillMaxWidth(), Icons.Rounded.DeleteForever, color = p.danger) { confirmReset = true }
            }
        }

        item {
            GlassCard {
                Caption("About")
                Spacer(Modifier.height(6.dp))
                Sub("GainForge 1.0 · works fully offline. All data is stored only on this phone.")
                Spacer(Modifier.height(4.dp))
                Sub("General fitness and nutrition guidance — not medical advice. Talk to a doctor before big diet or training changes, especially with a health condition.")
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            icon = { Icon(Icons.Rounded.Warning, null, tint = p.danger, modifier = Modifier.size(36.dp)) },
            title = {
                Text(
                    "DESTRUCTION AREA. ONLY DELETE ACCOUNT IF NECESSARY.",
                    color = p.danger, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    "This permanently deletes ${profile.name}'s whole profile and everything in it from this phone and the watch. " +
                        if (data.profiles.size > 1) "You'll switch to another profile." else "You'll start over at the questionnaire.",
                    color = p.danger
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    profile.photoPath?.let { runCatching { File(it).delete() } }
                    WorkoutSession.cancel()
                    AppState.update { d ->
                        val rest = d.profiles.filter { it.id != profile.id }
                        d.copy(profiles = rest, activeId = rest.firstOrNull()?.id)
                    }
                    confirmReset = false
                }) { Text("Delete profile", color = p.danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }

    deleteTarget?.let { t ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${t.name}?") },
            text = { Text("This removes their profile, logs and favorites from this phone. It can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    t.photoPath?.let { runCatching { File(it).delete() } }
                    AppState.update { d -> d.copy(profiles = d.profiles.filter { it.id != t.id }) }
                    deleteTarget = null
                }) { Text("Delete", color = p.danger) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

/** Copies the picked photo into private app storage, downscaled to ~800px and rotated upright. */
private fun savePhoto(ctx: Context, uri: Uri, id: String): String? = runCatching {
    val bmp: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri)) { decoder, info, _ ->
            val longest = maxOf(info.size.width, info.size.height)
            var sample = 1
            while (longest / (sample * 2) >= 800) sample *= 2
            decoder.setTargetSampleSize(sample)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 800) sample *= 2
        ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
    }
    val file = File(ctx.filesDir, "avatar_${id}_${System.currentTimeMillis()}.jpg")
    file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
    file.absolutePath
}.getOrNull()
