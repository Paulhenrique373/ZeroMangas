package com.example.zeromangas.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representa uma linha da tabela "produtos" no Supabase.
 * marca_id/categoria_id são as chaves estrangeiras; "marcas" e "categorias" são os dados
 * relacionados trazidos via join automático do Postgrest (select("*, marcas(nome), categorias(nome)")).
 */
@Serializable
data class ProdutoDto(
    val id: String = "",
    val nome: String = "",
    @SerialName("marca_id") val marcaId: String = "",
    @SerialName("categoria_id") val categoriaId: String = "",
    val volume: Int = 1,
    val preco: Double = 0.0,
    @SerialName("imagem_url") val imagemUrl: String = "",
    val descricao: String = "",
    @SerialName("em_destaque") val emDestaque: Boolean = false,
    val estoque: Int = 0,
    val marcas: NomeDto? = null,
    val categorias: NomeDto? = null
)

@Serializable
data class NomeDto(
    val nome: String = ""
)

@Serializable
data class MarcaDto(
    val id: String = "",
    val nome: String = ""
)

@Serializable
data class CategoriaDto(
    val id: String = "",
    val nome: String = ""
)