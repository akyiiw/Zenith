package br.com.zenith.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Desafio(
    val id: String = "",
    val titulo: String = "",
    val descricao: String? = null,
    @SerialName("criador_id") val criadorId: String = "",
    val tipo: String = "atividades",
    val meta: Double = 0.0,
    val unidade: String = "atividades",
    @SerialName("inicio_em") val inicioEm: String? = null,
    @SerialName("fim_em") val fimEm: String? = null,
    @SerialName("criado_em") val criadoEm: String? = null
)

@Serializable
data class DesafioParticipacao(
    @SerialName("desafio_id") val desafioId: String,
    @SerialName("user_id") val userId: String
)
