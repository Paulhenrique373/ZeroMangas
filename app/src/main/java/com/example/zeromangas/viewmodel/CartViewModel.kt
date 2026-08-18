package com.example.zeromangas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zeromangas.data.model.CartItem
import com.example.zeromangas.data.model.Cupom
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.data.model.Order
import com.example.zeromangas.data.repository.ViaCepRepository
import com.example.zeromangas.repository.CupomRepository
import com.example.zeromangas.repository.MangaRepository
import com.example.zeromangas.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val viaCepRepository = ViaCepRepository()
    private val cupomRepository = CupomRepository()
    private val mangaRepository = MangaRepository()

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

    private val _calculandoFrete = MutableStateFlow(false)
    val calculandoFrete: StateFlow<Boolean> = _calculandoFrete.asStateFlow()

    private val _cidadeUf = MutableStateFlow<String?>(null)
    val cidadeUf: StateFlow<String?> = _cidadeUf.asStateFlow()

    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState.asStateFlow()

    private val _avisoEstoque = MutableStateFlow<String?>(null)
    val avisoEstoque: StateFlow<String?> = _avisoEstoque.asStateFlow()

    // ---- Cupom de desconto ----

    private val _cupomInput = MutableStateFlow("")
    val cupomInput: StateFlow<String> = _cupomInput.asStateFlow()

    private val _cupomAplicado = MutableStateFlow<Cupom?>(null)
    val cupomAplicado: StateFlow<Cupom?> = _cupomAplicado.asStateFlow()

    private val _cupomErro = MutableStateFlow<String?>(null)
    val cupomErro: StateFlow<String?> = _cupomErro.asStateFlow()

    private val _validandoCupom = MutableStateFlow(false)
    val validandoCupom: StateFlow<Boolean> = _validandoCupom.asStateFlow()

    val desconto: StateFlow<Double> = combine(_itens, _cupomAplicado) { itens, cupom ->
        if (cupom == null) return@combine 0.0
        val subtotalAtual = itens.sumOf { it.subtotal }
        cupom.calcularDesconto(subtotalAtual)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun limparAvisoEstoque() {
        _avisoEstoque.value = null
    }

    fun adicionarItem(manga: Manga) {
        if (manga.estoque <= 0) {
            _avisoEstoque.value = "${manga.nome} está esgotado."
            return
        }

        val listaAtual = _itens.value
        val itemExistente = listaAtual.find { it.manga.id == manga.id }
        val quantidadeAtualNoCarrinho = itemExistente?.quantidade ?: 0

        if (quantidadeAtualNoCarrinho + 1 > manga.estoque) {
            _avisoEstoque.value = "Só temos ${manga.estoque} unidade(s) de ${manga.nome} em estoque."
            return
        }

        _itens.value = if (itemExistente != null) {
            listaAtual.map {
                if (it.manga.id == manga.id) it.copy(quantidade = it.quantidade + 1) else it
            }
        } else {
            listaAtual + CartItem(manga = manga, quantidade = 1)
        }
    }

    fun aumentarQuantidade(manga: Manga) {
        val itemAtual = _itens.value.find { it.manga.id == manga.id } ?: return

        if (itemAtual.quantidade + 1 > manga.estoque) {
            _avisoEstoque.value = "Só temos ${manga.estoque} unidade(s) de ${manga.nome} em estoque."
            return
        }

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
        _cidadeUf.value = null
    }

    /**
     * Calcula o frete consultando o CEP real na API do ViaCEP.
     * O valor do frete é definido pela região (UF) do endereço encontrado,
     * simulando a variação de preço por distância que uma transportadora real cobraria.
     */
    fun calcularFrete() {
        val digitos = _cep.value.filter { it.isDigit() }

        if (digitos.length != 8) {
            _cepErro.value = "Informe um CEP válido com 8 números."
            _frete.value = null
            _cidadeUf.value = null
            return
        }

        _cepErro.value = null
        _calculandoFrete.value = true
        _frete.value = null
        _cidadeUf.value = null

        viewModelScope.launch {
            val resultado = viaCepRepository.buscarEndereco(digitos)
            resultado.fold(
                onSuccess = { endereco ->
                    _frete.value = valorFretePorUf(endereco.uf)
                    _cidadeUf.value = "${endereco.cidade} - ${endereco.uf}"
                    _cepErro.value = null
                },
                onFailure = { erro ->
                    _cepErro.value = erro.message ?: "Não foi possível calcular o frete."
                    _frete.value = null
                    _cidadeUf.value = null
                }
            )
            _calculandoFrete.value = false
        }
    }

    /**
     * Valor de frete por região, simulando a distância real a partir do centro de distribuição (São Paulo).
     */
    private fun valorFretePorUf(uf: String): Double {
        val sudeste = setOf("SP", "RJ", "MG", "ES")
        val sulECentroOeste = setOf("PR", "SC", "RS", "MT", "MS", "GO", "DF")

        return when (uf.uppercase()) {
            in sudeste -> 12.0
            in sulECentroOeste -> 18.0
            else -> 25.0 // Norte e Nordeste
        }
    }

    fun atualizarCupomInput(valor: String) {
        _cupomInput.value = valor
        _cupomErro.value = null
    }

    /**
     * Valida o código digitado contra a coleção "cupons" do Firestore e,
     * se válido, aplica o desconto sobre o subtotal atual do carrinho.
     */
    fun aplicarCupom() {
        val codigo = _cupomInput.value.trim()

        if (codigo.isBlank()) {
            _cupomErro.value = "Informe um código de cupom."
            return
        }

        _cupomErro.value = null
        _validandoCupom.value = true

        viewModelScope.launch {
            val resultado = cupomRepository.buscarCupom(codigo)
            resultado.fold(
                onSuccess = { cupom ->
                    val subtotalAtual = _itens.value.sumOf { it.subtotal }
                    if (subtotalAtual < cupom.valorMinimo) {
                        _cupomErro.value = "Este cupom exige compra mínima de R$ ${"%.2f".format(cupom.valorMinimo)}."
                        _cupomAplicado.value = null
                    } else {
                        _cupomAplicado.value = cupom
                        _cupomErro.value = null
                    }
                },
                onFailure = { erro ->
                    _cupomAplicado.value = null
                    _cupomErro.value = erro.message ?: "Não foi possível validar o cupom."
                }
            )
            _validandoCupom.value = false
        }
    }

    fun removerCupom() {
        _cupomAplicado.value = null
        _cupomInput.value = ""
        _cupomErro.value = null
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
            // 1. Buscar o estoque real e atualizado do banco para todos os produtos do carrinho.
            val produtoIds = itensAtuais.map { it.manga.id }
            val resultadoEstoque = mangaRepository.buscarEstoqueAtual(produtoIds)

            val estoqueAtualMap = resultadoEstoque.getOrElse {
                _checkoutState.value = CheckoutState.Erro("Não foi possível verificar o estoque. Tente novamente.")
                return@launch
            }

            // 2. Validar cada item do carrinho contra o estoque real.
            for (item in itensAtuais) {
                val estoqueDisponivel = estoqueAtualMap[item.manga.id]

                if (estoqueDisponivel == null) {
                    _checkoutState.value = CheckoutState.Erro(
                        "${item.manga.nome} não está mais disponível."
                    )
                    return@launch
                }

                if (estoqueDisponivel < item.quantidade) {
                    _checkoutState.value = CheckoutState.Erro(
                        "Estoque insuficiente para ${item.manga.nome}. Disponível: $estoqueDisponivel unidade(s)."
                    )
                    return@launch
                }
            }

            // 3. Estoque validado: seguir com o fluxo normal de criação do pedido.
            val subtotalAtual = itensAtuais.sumOf { it.subtotal }
            val cupomAtual = _cupomAplicado.value
            val descontoAtual = cupomAtual?.calcularDesconto(subtotalAtual) ?: 0.0

            val pedido = Order(
                userId = userId,
                itens = itensAtuais,
                valorProdutos = subtotalAtual,
                valorFrete = freteAtual,
                valorDesconto = descontoAtual,
                valorTotal = subtotalAtual + freteAtual - descontoAtual,
                tipoFrete = "ViaCEP",
                cep = _cep.value,
                cupomCodigo = cupomAtual?.codigo ?: "",
                data = System.currentTimeMillis(),
                status = "PROCESSANDO"
            )

            val resultado = orderRepository.salvarPedido(pedido)
            resultado.fold(
                onSuccess = { pedidoId ->
                    // 4. Pedido salvo com sucesso: descontar a quantidade comprada do estoque de cada produto.
                    for (item in itensAtuais) {
                        val estoqueAntesDaCompra = estoqueAtualMap[item.manga.id] ?: continue
                        mangaRepository.descontarEstoque(
                            produtoId = item.manga.id,
                            quantidadeComprada = item.quantidade,
                            estoqueAtual = estoqueAntesDaCompra
                        )
                    }

                    _checkoutState.value = CheckoutState.Sucesso(pedidoId)
                    limparCarrinho()
                    _cep.value = ""
                    _frete.value = null
                    _cidadeUf.value = null
                    removerCupom()
                },
                onFailure = {
                    _checkoutState.value = CheckoutState.Erro("Não foi possível finalizar a compra. Tente novamente.")
                }
            )
        }
    }
}