package com.example.zeromangas.data.model

data class Order(
    val id: String = "",
    val userId: String = "",
    val itens: List<CartItem> = emptyList(),
    val valorProdutos: Double = 0.0,
    val valorFrete: Double = 0.0,
    val valorTotal: Double = 0.0,
    val tipoFrete: String = "",
    val cep: String = "",
    val data: Long = System.currentTimeMillis(),
    val status: String = "Concluído"
)