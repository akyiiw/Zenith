package br.com.zenith.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AchievementUnlock(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val slug: String,
    @SerialName("desbloqueada_em") val unlockedAt: String? = null
)
