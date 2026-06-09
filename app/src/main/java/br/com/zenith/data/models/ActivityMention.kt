package br.com.zenith.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityMention(
    val id: String,
    @SerialName("activity_id") val activityId: String,
    @SerialName("publisher_id") val publisherId: String,
    @SerialName("mentioned_user_id") val mentionedUserId: String,
    val status: String = STATUS_PENDING,
    @SerialName("show_on_mentioned_profile") val showOnMentionedProfile: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("responded_at") val respondedAt: String? = null,
    @SerialName("atividades") val activity: Atividade? = null,
    @SerialName("publisher") val publisher: Profile? = null,
    @SerialName("mentioned_user") val mentionedUser: Profile? = null
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_DECLINED = "declined"
    }
}

@Serializable
data class ActivityMentionInsert(
    @SerialName("activity_id") val activityId: String,
    @SerialName("publisher_id") val publisherId: String,
    @SerialName("mentioned_user_id") val mentionedUserId: String,
    val status: String = ActivityMention.STATUS_PENDING,
    @SerialName("show_on_mentioned_profile") val showOnMentionedProfile: Boolean = true
)
