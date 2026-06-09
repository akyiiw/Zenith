package br.com.zenith.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonalGoal(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val title: String,
    val metric: String, // 'distance_km', 'steps', 'active_minutes', 'activities', 'sleep_hours'
    val period: String, // 'weekly', 'monthly'
    @SerialName("target_value") val targetValue: Double,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
