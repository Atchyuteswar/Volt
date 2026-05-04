package com.kazexyt.volt.utils

import com.kazexyt.volt.supabase

// 1. The crucial import that enables 'supabase.functions'
import io.github.jan.supabase.functions.functions

// 2. Explicit Ktor imports for the request builder and response
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

// 3. Kotlinx serialization imports
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object AiCoachManager {

    suspend fun generateWeeklyDebrief(
        avgCalories: Int,
        avgProtein: Int,
        waterConsistency: String
    ): String? {
        return try {
            // We construct a simple sentence summarizing the week to save bandwidth
            val summaryData = "This week, I averaged $avgCalories calories per day, $avgProtein grams of protein per day, and my hydration was $waterConsistency."

            // Build the JSON payload
            val payloadString = buildJsonObject {
                put("weeklyData", summaryData)
            }.toString()

            // Explicitly declare HttpResponse type and use Ktor's setBody
            val response: HttpResponse = supabase.functions.invoke("generate-debrief") {
                contentType(ContentType.Application.Json)
                setBody(payloadString)
            }

            // Extract the string using Ktor's native method
            val jsonString = response.bodyAsText()

            // Parse the JSON to extract just the "debrief" string
            val jsonElement = Json.parseToJsonElement(jsonString)
            jsonElement.jsonObject["debrief"]?.jsonPrimitive?.content

        } catch (e: Exception) {
            // Logs the error for developers
            android.util.Log.e("VoltCoach", "Debrief failed: ${e.message}")

            // Returns the error to the UI so you can see it!
            "Oops! Volt couldn't connect. Error: ${e.message}"
        }
    }
}