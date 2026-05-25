package br.com.zenith.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UsuarioTitulo(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("titulo_id") val tituloId: String,
    @SerialName("desbloqueado_em") val desbloqueadoEm: String? = null
)