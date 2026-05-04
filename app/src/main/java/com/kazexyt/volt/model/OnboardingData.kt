package com.kazexyt.volt.model

import kotlinx.serialization.Serializable

data class VoltOnboardingData(
    var name: String = "",
    var gender: String = "",
    var birthDate: String = "",
    var units: String = "Metric",
    var height: String = "",
    var weight: String = "",
    var activityLevel: String = "",
    var exerciseFreq: String = "",
    var diet: String = "",
    var primaryGoal: String = "",
    var improvementGoals: Set<String> = emptySet(),
    var history: String = "",
    var obstacles: Set<String> = emptySet(),
    var commitment: String = "",
    var trackingPrefs: String = ""
)

@Serializable
data class ProfileDatabaseRow(
    val id: String,
    val name: String,
    val gender: String,
    val birth_date: String,
    val units: String,
    val height: String,
    val weight: String,
    val activity_level: String,
    val exercise_freq: String,
    val diet: String,
    val primary_goal: String,
    val improvement_goals: List<String>,
    val history: String,
    val obstacles: List<String>,
    val commitment: String,
    val tracking_prefs: String,
    val daily_goal: Int = 2200
)
