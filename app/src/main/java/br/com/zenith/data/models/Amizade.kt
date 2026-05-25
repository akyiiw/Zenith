package br.com.zenith.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Amizade(
    @SerialName("user_id") val userId: String,
    @SerialName("amigo_id") val friendId: String,
    val status: String
)

@Serializable
data class AmizadeInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("amigo_id") val friendId: String,
    val status: String
)
