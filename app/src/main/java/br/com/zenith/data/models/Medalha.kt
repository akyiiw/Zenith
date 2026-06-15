package br.com.zenith.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Medalha(
    @SerialName("id_medalha") val id: Int,
    val colocacao: String? = null,
    val vencedor: Boolean? = null
)

@Serializable
data class UsuarioMedalha(
    @SerialName("id_medalha") val medalhaId: Int,
    @SerialName("id_usuario") val userId: String,
    @SerialName("medalhas") val medalha: Medalha? = null
)
