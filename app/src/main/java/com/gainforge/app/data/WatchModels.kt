package com.gainforge.app.data

import kotlinx.serialization.Serializable

// Keep these field names identical to the copies in the :wear module.

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
