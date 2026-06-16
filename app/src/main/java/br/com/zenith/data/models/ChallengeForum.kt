package br.com.zenith.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChallengeForumPost(
    val id: String,
    @SerialName("author_id") val authorId: String,
    val content: String? = null,
    @SerialName("desafio_id") val desafioId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ChallengeForumMedia(
    val id: String,
    @SerialName("post_id") val postId: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("storage_path") val storagePath: String,
    val position: Int = 0,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ChallengeForumEntry(
    val id: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("entry_type") val entryType: String,
    @SerialName("post_id") val postId: String? = null,
    @SerialName("activity_id") val activityId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class FeedEntryView(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("feed_entry_id") val feedEntryId: String,
    @SerialName("viewed_at") val viewedAt: String? = null
)

@Serializable
data class ChallengeForumComment(
    val id: String,
    @SerialName("entry_id") val entryId: String,
    @SerialName("author_id") val authorId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null
)
