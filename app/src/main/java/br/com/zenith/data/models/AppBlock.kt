package br.com.zenith.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppBlock(
    @SerialName("id_bloqueio") val id: Int,
    @SerialName("id_usuario") val userId: String,
    @SerialName("nome_aplicativo") val appName: String,
    @SerialName("hora_inicio") val startTime: String,
    @SerialName("hora_conclusao") val endTime: String,
    @SerialName("status_bloqueio") val active: Boolean = true,
    @SerialName("dias_bloqueados") val blockedDays: String? = null,
    val lembrete: Boolean? = null
)
