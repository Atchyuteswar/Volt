package com.kazexyt.volt.utils

import com.kazexyt.volt.model.MealLog
import com.kazexyt.volt.supabase
import io.github.jan.supabase.functions.functions
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.*
import java.util.UUID
import java.time.OffsetDateTime

object AiMealParser {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun parseSpokenMeal(spokenText: String): MealLog {
        // Use "parse-meal" for voice/text logs
        return callParseMeal("parse-meal", mapOf("spokenText" to spokenText))
    }

    suspend fun parseImage(imageFile: java.io.File): MealLog {
        // 1. Process and resize the image for the AI lens
        val options = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath, options)

        var inSampleSize = 1
        val maxDim = 1024
        if (options.outHeight > maxDim || options.outWidth > maxDim) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while (halfHeight / inSampleSize >= maxDim && halfWidth / inSampleSize >= maxDim) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = android.graphics.BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        val bitmap = android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath, decodeOptions)
            ?: throw Exception("Could not process the captured image.")

        // 2. Compress to JPEG
        val out = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
        val compressedBytes = out.toByteArray()

        // 3. Encode to raw Base64 (Standard for gemini-vision endpoint)
        val base64Image = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)

        android.util.Log.d("VoltAI", "Sending image to Vision: ${compressedBytes.size} bytes")

        // 📍 FIXED: Target "gemini-vision" for photos
        return callParseMeal("gemini-vision", mapOf(
            "image" to base64Image
        ))
    }

    private suspend fun callParseMeal(functionName: String, payload: Map<String, String>): MealLog {
        val payloadString = buildJsonObject {
            payload.forEach { (k, v) -> put(k, v) }
        }.toString()

        val response = try {
            supabase.functions.invoke(functionName) {
                contentType(ContentType.Application.Json)
                setBody(payloadString)
            }
        } catch (e: Exception) {
            android.util.Log.e("VoltAI", "Supabase $functionName Error", e)
            if (e.message?.contains("503") == true) {
                throw Exception("Aira is warming up. Please wait 10 seconds.")
            }
            throw e
        }

        val rawString = response.bodyAsText()

        // Clean JSON from Markdown backticks
        val cleanJson = rawString
            .replace("```json", "", ignoreCase = true)
            .replace("```", "")
            .trim()

        if (!cleanJson.startsWith("{")) {
            throw Exception("Aira could not identify that food. Try a clearer photo.")
        }

        return try {
            val jsonObject = jsonParser.parseToJsonElement(cleanJson).jsonObject

            fun getMacro(key: String): Int {
                val element = jsonObject[key]?.jsonPrimitive
                return element?.intOrNull ?: element?.contentOrNull?.toFloatOrNull()?.toInt() ?: 0
            }

            MealLog(
                id = UUID.randomUUID().toString(),
                user_id = "", // Filled by Supabase on save
                food_name = jsonObject["food_name"]?.jsonPrimitive?.contentOrNull ?: "Unknown Meal",
                calories = getMacro("calories"),
                protein = getMacro("protein"),
                carbs = getMacro("carbs"),
                fat = getMacro("fat"),
                created_at = OffsetDateTime.now().toString()
            )
        } catch (_: Exception) {
            throw Exception("Data error. Please try logging again.")
        }
    }
}