package com.kazexyt.volt.model

import kotlinx.serialization.Serializable

@Serializable
data class WaterLog(
    val id: String = "", // Supabase UUIDs map to Strings
    val user_id: String,
    val amount: Int,       // Matches your 'amount int4' column perfectly
    val created_at: String? = null
)