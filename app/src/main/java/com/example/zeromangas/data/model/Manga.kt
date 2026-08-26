package com.example.zeromangas.data.model

data class Manga(
    val id: String = "",
    val nome: String = "",
    val marca: String = "",
    val categoria: String = "",
    val volume: Int = 1,
    val preco: Double = 0.0,
    val imagemUrl: String = "",
    val descricao: String = "",
    val emDestaque: Boolean = false,
    val estoque: Int = 10,
    val autor: String = "",
    val notaMedia: Double = 0.0,
    val totalAvaliacoes: Int = 0,
    val precoPromocional: Double? = null,
    val emPromocao: Boolean = false
)