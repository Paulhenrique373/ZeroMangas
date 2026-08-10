package com.example.zeromangas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zeromangas.data.model.CartItem
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.data.model.Order
import com.example.zeromangas.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

sealed class CheckoutState {
    object Idle : CheckoutState()
    object Carregando : CheckoutState()
    data class Sucesso(val pedidoId: String) : CheckoutState()
    data class Erro(val mensagem: String) : CheckoutState()
}

class CartViewModel : ViewModel() {

    private val orderRepository = OrderRepository()

    private val _itens = MutableStateFlow<List<CartItem>>(emptyList())
    val itens: StateFlow<List<CartItem>> = _itens.asStateFlow()

    val subtotal: StateFlow<Double> = _itens
        .map { itens -> itens.sumOf { it.subtotal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _cep = MutableStateFlow("")
    val cep: StateFlow<String> = _cep.asStateFlow()

    private val _cepErro = MutableStateFlow<String?>(null)
    val cepErro: StateFlow<String?> = _cepErro.asStateFlow()

    private val _frete = MutableStateFlow<Double?>(null)
    val frete: StateFlow<Double?> = _frete.asStateFlow()

    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState.asStateFlow()

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

    fun atualizarCep(valor: String) {
        _cep.value = valor
        _frete.value = null
        _cepErro.value = null
    }

    /**
     * Cálculo de frete simulado (sem API externa), porém determinístico:
     * o valor do frete depende do primeiro dígito do CEP informado.
     */
    fun calcularFrete() {
        val digitos = _cep.value.filter { it.isDigit() }

        if (digitos.length != 8) {
            _cepErro.value = "Informe um CEP válido com 8 números."
            _frete.value = null
            return
        }

        _cepErro.value = null
        val primeiroDigito = digitos.first().digitToInt()
        _frete.value = 10.0 + (primeiroDigito * 2.0)
    }

    fun resetarCheckout() {
        _checkoutState.value = CheckoutState.Idle
    }

    fun finalizarCompra(userId: String) {
        val itensAtuais = _itens.value

        if (itensAtuais.isEmpty()) {
            _checkoutState.value = CheckoutState.Erro("Seu carrinho está vazio.")
            return
        }

        val freteAtual = _frete.value
        if (freteAtual == null) {
            _checkoutState.value = CheckoutState.Erro("Calcule o frete informando seu CEP antes de finalizar a compra.")
            return
        }

        if (userId.isBlank()) {
            _checkoutState.value = CheckoutState.Erro("Não foi possível finalizar a compra. Tente novamente.")
            return
        }

        _checkoutState.value = CheckoutState.Carregando

        viewModelScope.launch {
            val subtotalAtual = itensAtuais.sumOf { it.subtotal }
            val pedido = Order(
                userId = userId,
                itens = itensAtuais,
                valorProdutos = subtotalAtual,
                valorFrete = freteAtual,
                valorTotal = subtotalAtual + freteAtual,
                tipoFrete = "Simulado",
                cep = _cep.value,
                data = System.currentTimeMillis(),
                status = "PROCESSANDO"
            )

            val resultado = orderRepository.salvarPedido(pedido)
            resultado.fold(
                onSuccess = { pedidoId ->
                    _checkoutState.value = CheckoutState.Sucesso(pedidoId)
                    limparCarrinho()
                    _cep.value = ""
                    _frete.value = null
                },
                onFailure = {
                    _checkoutState.value = CheckoutState.Erro("Não foi possível finalizar a compra. Tente novamente.")
                }
            )
        }
    }
}
