package br.com.zenith.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Titulo(
    val id: String,
    val slug: String,
    val nome: String,
    val cor: String = "#238D25",
    val descricao: String? = null
)
