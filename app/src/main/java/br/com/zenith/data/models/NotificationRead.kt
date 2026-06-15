package br.com.zenith.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationRead(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("notification_id") val notificationId: String,
    @SerialName("read_at") val readAt: String? = null
)
