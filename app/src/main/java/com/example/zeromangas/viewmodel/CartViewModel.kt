package com.example.zeromangas.viewmodel

import androidx.lifecycle.ViewModel
import com.example.zeromangas.data.model.CartItem
import com.example.zeromangas.data.model.Manga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CartViewModel : ViewModel() {

    private val _itens = MutableStateFlow<List<CartItem>>(emptyList())
    val itens: StateFlow<List<CartItem>> = _itens.asStateFlow()

    fun adicionarItem(manga: Manga) {
        val listaAtual = _itens.value
        val itemExistente = listaAtual.find { it.manga.id == manga.id }

        _itens.value = if (itemExistente != null) {
            listaAtual.map {
                if (it.manga.id == manga.id) it.copy(quantidade = it.quantidade + 1) else it
            }
        } else {
            listaAtual + CartItem(manga = manga, quantidade = 1)
        }
    }

    fun aumentarQuantidade(manga: Manga) {
        _itens.value = _itens.value.map {
            if (it.manga.id == manga.id) it.copy(quantidade = it.quantidade + 1) else it
        }
    }

    fun diminuirQuantidade(manga: Manga) {
        _itens.value = _itens.value.mapNotNull {
            if (it.manga.id == manga.id) {
                if (it.quantidade > 1) it.copy(quantidade = it.quantidade - 1) else null
            } else it
        }
    }

    fun removerItem(manga: Manga) {
        _itens.value = _itens.value.filterNot { it.manga.id == manga.id }
    }

    fun limparCarrinho() {
        _itens.value = emptyList()
    }
}