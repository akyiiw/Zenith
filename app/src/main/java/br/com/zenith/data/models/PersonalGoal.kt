package br.com.zenith.data.models

import kotlinx.serialization.Serializable

@Serializable
data class PersonalGoal(
    val id: String? = null,
    val userId: String,
    val title: String,
    val metric: String, // 'distance_km', 'steps', 'active_minutes', 'activities', 'sleep_hours'
    val period: String, // 'weekly', 'monthly'
    val targetValue: Double,
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
