package com.kazexyt.volt.utils

import com.kazexyt.volt.model.VoltOnboardingData

object GoalCalculator {
    fun calculateDailyGoal(data: VoltOnboardingData): Int {
        return try {
            val weight = data.weight.toDoubleOrNull() ?: 70.0
            val height = data.height.toDoubleOrNull() ?: 170.0

            // Simple age calculation from MM/DD/YYYY
            val year = data.birthDate.split("/").lastOrNull()?.toIntOrNull() ?: 2000
            val age = 2026 - year

            // 1. Calculate BMR (Mifflin-St Jeor)
            val bmr = if (data.gender == "Male") {
                (10 * weight) + (6.25 * height) - (5 * age) + 5
            } else {
                (10 * weight) + (6.25 * height) - (5 * age) - 161
            }

            // 2. Activity Multiplier
            val multiplier = when (data.activityLevel) {
                "Mostly sitting" -> 1.2
                "Lightly active" -> 1.375
                "Moderately active" -> 1.55
                "Very active" -> 1.725
                "Extremely active" -> 1.9
                else -> 1.2
            }

            val tdee = bmr * multiplier

            // 3. Goal Adjustment
            val finalGoal = when (data.primaryGoal) {
                "Lose weight" -> tdee - 500
                "Gain muscle" -> tdee + 300
                else -> tdee
            }

            finalGoal.toInt().coerceIn(1200, 5000) // Safety bounds
        } catch (e: Exception) {
            2200 // Fallback
        }
    }
}
