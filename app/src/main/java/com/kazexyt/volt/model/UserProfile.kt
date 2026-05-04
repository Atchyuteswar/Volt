package com.kazexyt.volt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String = "",
    val name: String? = null,
    val daily_goal: Int = 2200,
    val daily_water_ml: Int = 0,
    val updated_at: String? = null,
    val gender: String,
    val created_at: String? = null,
    @SerialName("birth_date")
    val birthDate: String,
)
