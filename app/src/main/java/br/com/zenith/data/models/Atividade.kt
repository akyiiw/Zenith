package br.com.zenith.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Atividade(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("exercicio_id") val exercicioId: String,
    val titulo: String? = null,
    val valor: Double,
    @SerialName("duracao_min") val duracaoMin: Int? = null,
    val nota: Int? = null,
    val humor: String? = null,
    val intensidade: String? = null,
    val verificada: Boolean = false,
    val clima: String? = null,
    @SerialName("foto_hash") val fotoHash: String? = null,
    @SerialName("desafio_id") val desafioId: String? = null,
    val rota: String? = null,
    @SerialName("realizada_em") val realizadaEm: String? = null,
    @SerialName("criada_em") val criadaEm: String? = null,

    // join com exercicios
    @SerialName("exercicios")
    val exercicio: Exercicio? = null
)
