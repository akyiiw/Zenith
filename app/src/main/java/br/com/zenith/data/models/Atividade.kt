package br.com.zenith.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Atividade(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("exercicio_id") val exercicioId: String,
    val titulo: String? = null,
    val descricao: String? = null,
    val valor: Double,
    @SerialName("duracao_min") val duracaoMin: Int? = null,
    val nota: Int? = null,
    val humor: String? = null,
    val intensidade: String? = null,
    val verificada: Boolean = false,
    val clima: String? = null,
    @SerialName("foto_hash") val fotoHash: String? = null,
    @SerialName("desafio_id") val desafioId: String? = null,
    val passos: Int? = null,
    @SerialName("distancia_bruta") val distanciaBruta: Double? = null,
    @SerialName("gps_accuracy_media") val gpsAccuracyMedia: Double? = null,
    @SerialName("gps_pontos_aceitos") val gpsPontosAceitos: Int? = null,
    @SerialName("gps_pontos_rejeitados") val gpsPontosRejeitados: Int? = null,
    @SerialName("gps_qualidade") val gpsQualidade: String? = null,
    val rota: String? = null,
    @SerialName("realizada_em") val realizadaEm: String? = null,
    @SerialName("criada_em") val criadaEm: String? = null,

    @SerialName("exercicios")
    val exercicio: Exercicio? = null
)
