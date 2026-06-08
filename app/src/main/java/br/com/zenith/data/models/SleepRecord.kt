package br.com.zenith.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SleepRecord(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    val quality: Int? = null,
    val source: String = "manual",
    @SerialName("created_at") val createdAt: String? = null
)
