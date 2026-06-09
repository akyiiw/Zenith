package br.com.zenith.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val name: String,
    @SerialName("displayName") val displayName: String,
    @SerialName("userAge") val userAge: Int? = null,
    @SerialName("pictureHash") val pictureHash: String? = null,
    @SerialName("bannerHash") val bannerHash: String? = null,
    @SerialName("aboutMe") val aboutMe: String? = null,
    @SerialName("isPremium") val isPremium: Boolean = false,
    @SerialName("registerDate") val registerDate: String? = null,
    @SerialName("accountStatus") val accountStatus: Boolean = true,
    val email: String,
    val streak: Int = 0,
    @SerialName("last_streak_activity_date") val lastStreakActivityDate: String? = null,
    val status: String? = null,
    @SerialName("titulo_id") val tituloId: String? = null,
    @SerialName("titulos") val titulo: Titulo? = null,
    @SerialName("badge_id") val badgeId: Int? = null,
    @SerialName("name_updated_at") val nameUpdatedAt: String? = null

)
