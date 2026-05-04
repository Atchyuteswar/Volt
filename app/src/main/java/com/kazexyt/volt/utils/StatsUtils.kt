package com.kazexyt.volt.utils

import com.kazexyt.volt.model.MealLog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object StatsUtils {

    // 1. Calculate the Streak (Consecutive days with at least 1 log)
    fun calculateStreak(meals: List<MealLog>): Int {
        if (meals.isEmpty()) return 0

        // Extract just the "YYYY-MM-DD" part and get unique days sorted descending (newest first)
        val loggedDates = meals.mapNotNull { it.created_at?.take(10) }
            .distinct()
            .sortedDescending()

        if (loggedDates.isEmpty()) return 0

        var streak = 0
        var currentDate = LocalDate.now()

        // Check if they logged today. If not, check if they logged yesterday (streak might still be alive)
        val todayStr = currentDate.toString()
        val yesterdayStr = currentDate.minusDays(1).toString()

        if (!loggedDates.contains(todayStr) && !loggedDates.contains(yesterdayStr)) {
            return 0 // Streak is dead
        }

        // Count backward
        for (dateStr in loggedDates) {
            if (dateStr == currentDate.toString() || dateStr == currentDate.minusDays(1).also { currentDate = it }.toString()) {
                streak++
                currentDate = LocalDate.parse(dateStr) // align current date to the found log
            } else {
                break // Gap found, streak ends
            }
        }
        return streak
    }

    // 2. Evolution Bars: Get the last 7 days of Calories
    fun getWeeklyEvolution(meals: List<MealLog>): List<Pair<String, Int>> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("EEE") // "Mon", "Tue", etc.

        // Build a list of the last 7 days, defaulting to 0 calories
        return (6 downTo 0).map { daysAgo ->
            val targetDate = today.minusDays(daysAgo.toLong())
            val targetDateString = targetDate.toString()

            // Sum calories for this specific day
            val dailyCalories = meals
                .filter { it.created_at?.startsWith(targetDateString) == true }
                .sumOf { it.calories }

            Pair(targetDate.format(formatter), dailyCalories)
        }
    }

    // 3. Analytics Averages (e.g., Average Daily Calories over all time)
    fun getAverageCalories(meals: List<MealLog>): Int {
        if (meals.isEmpty()) return 0
        val uniqueDays = meals.mapNotNull { it.created_at?.take(10) }.distinct().size
        val totalCalories = meals.sumOf { it.calories }
        return if (uniqueDays > 0) totalCalories / uniqueDays else 0
    }
}