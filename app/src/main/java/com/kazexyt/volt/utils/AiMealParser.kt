package com.kazexyt.volt.utils

import com.kazexyt.volt.model.MealLog
import com.kazexyt.volt.supabase
import io.github.jan.supabase.functions.functions
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.*

object AiMealParser {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // 📍 Notice: Returns MealLog directly, not MealLog?
    suspend fun parseSpokenMeal(spokenText: String): MealLog {
        return callParseMeal(mapOf("spokenText" to spokenText))
    }

    suspend fun parseImage(imageFile: java.io.File): MealLog {
        val bytes = imageFile.readBytes()
        val base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        return callParseMeal(mapOf(
            "image" to base64Image,
            "spokenText" to "Image-based parsing request"
        ))
    }

    private suspend fun callParseMeal(payload: Map<String, String>): MealLog {
        android.util.Log.d("VoltVoice", "Sending to Aira: $payload")

        val payloadString = buildJsonObject {
            payload.forEach { (k, v) -> put(k, v) }
        }.toString()

        val response = supabase.functions.invoke("parse-meal") {
            contentType(ContentType.Application.Json)
            setBody(payloadString)
        }

        val rawString = response.bodyAsText()
        android.util.Log.d("VoltVoice", "Aira replied: $rawString")

        // 2. CLEAN THE MARKDOWN
        val cleanJson = rawString
            .replace("```json", "", ignoreCase = true)
            .replace("```", "")
            .trim()

        // 3. SAFETY CHECK: Did the server return an error instead of JSON?
        if (!cleanJson.startsWith("{")) {
            android.util.Log.e("VoltVoice", "Invalid JSON from Aira: $cleanJson")
            throw Exception("Aira was unable to parse that meal. Please try again with more detail.")
        }

        // 4. BULLETPROOF PARSING
        return try {
            val jsonObject = jsonParser.parseToJsonElement(cleanJson).jsonObject

            fun getMacro(key: String): Int {
                val element = jsonObject[key]?.jsonPrimitive
                return element?.intOrNull ?: element?.contentOrNull?.toFloatOrNull()?.toInt() ?: 0
            }

            MealLog(
                id = java.util.UUID.randomUUID().toString(),
                user_id = "",
                food_name = jsonObject["food_name"]?.jsonPrimitive?.contentOrNull ?: "Custom Meal",
                calories = getMacro("calories"),
                protein = getMacro("protein"),
                carbs = getMacro("carbs"),
                fat = getMacro("fat"),
                created_at = java.time.OffsetDateTime.now().toString()
            )
        } catch (e: Exception) {
            android.util.Log.e("VoltVoice", "Parsing error", e)
            throw Exception("Aira had trouble organizing that meal data. Please try again.")
        }
    }
}