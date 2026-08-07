package com.example.zeromangas.data.model

data class CartItem(
    val manga: Manga = Manga(),
    val quantidade: Int = 1
) {
    val subtotal: Double
        get() = manga.preco * quantidade
}