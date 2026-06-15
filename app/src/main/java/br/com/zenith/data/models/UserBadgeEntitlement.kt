package br.com.zenith.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserBadgeEntitlement(
    @SerialName("user_id") val userId: String,
    @SerialName("badge_id") val badgeId: Int,
    val source: String,
    val badge: Badge? = null
)
