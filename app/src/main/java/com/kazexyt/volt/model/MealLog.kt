package com.kazexyt.volt.model

import kotlinx.serialization.Serializable
import java.util.UUID
import java.time.OffsetDateTime

@Serializable
data class MealLog(
    val food_name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val user_id: String? = null,
    val id: String = UUID.randomUUID().toString(),
    val created_at: String = OffsetDateTime.now().toString()
)
