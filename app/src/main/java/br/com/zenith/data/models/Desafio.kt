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
    @SerialName("criado_em") val criadoEm: String? = null,
    @SerialName("banner_hash") val bannerHash: String? = null,
    @SerialName("atividade_designada") val atividadeDesignada: String = "caminhada",
    @SerialName("apenas_premium") val apenasPremium: Boolean = false,
    @SerialName("modo_meta") val modoMeta: String = "fixa",
    val metrica: String = "distancia",
    val visibilidade: String = "publico",
    @SerialName("max_participantes") val maxParticipantes: Int? = null,
    @SerialName("aceita_registro_manual") val aceitaRegistroManual: Boolean = false,
    @SerialName("ranking_tipo") val rankingTipo: String = "menor_tempo",
    @SerialName("objetivo_metrica") val objetivoMetrica: String? = "distancia",
    @SerialName("objetivo_valor") val objetivoValor: Double? = null
)

@Serializable
data class DesafioParticipacao(
    @SerialName("desafio_id") val desafioId: String,
    @SerialName("user_id") val userId: String
)
