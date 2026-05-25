package br.com.zenith.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Exercicio(
    val id: String,
    val slug: String,
    val nome: String,
    val unidade: String,
    val grupo: String? = null,
    val icone: String? = null
)
