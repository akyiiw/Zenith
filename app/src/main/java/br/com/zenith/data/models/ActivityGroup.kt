package br.com.zenith.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityGroup(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val description: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ActivityGroupItem(
    @SerialName("group_id") val groupId: String,
    @SerialName("activity_id") val activityId: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ActivityLink(
    @SerialName("source_activity_id") val sourceActivityId: String,
    @SerialName("target_activity_id") val targetActivityId: String,
    @SerialName("link_type") val linkType: String,
    @SerialName("created_at") val createdAt: String? = null
) {
    companion object {
        const val TYPE_PARTICIPATION = "participation"
        const val TYPE_RELATED = "related"
    }
}
