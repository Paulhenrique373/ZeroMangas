package com.example.zeromangas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zeromangas.data.model.CartItem
import com.example.zeromangas.data.model.Cupom
import com.example.zeromangas.data.model.Endereco
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.data.model.Order
import com.example.zeromangas.repository.CarrinhoRepository
import com.example.zeromangas.repository.EnderecoCep
import com.example.zeromangas.repository.ViaCepRepository
import com.example.zeromangas.repository.CupomRepository
import com.example.zeromangas.repository.EnderecoRepository
import com.example.zeromangas.repository.MangaRepository
import com.example.zeromangas.repository.OrderRepository
import com.example.zeromangas.repository.UsuarioRepository
import kotlinx.coroutines.delay
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
    private val usuarioRepository = UsuarioRepository()
    private val enderecoRepository = EnderecoRepository()
    private val carrinhoRepository = CarrinhoRepository()

    private var usuarioIdAtual: String? = null

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

    private val _enderecoEncontrado = MutableStateFlow<EnderecoCep?>(null)

    private var clienteIdCache: String? = null

    private val _enderecosSalvos = MutableStateFlow<List<Endereco>>(emptyList())
    val enderecosSalvos: StateFlow<List<Endereco>> = _enderecosSalvos.asStateFlow()

    private val _carregandoEnderecosSalvos = MutableStateFlow(false)
    val carregandoEnderecosSalvos: StateFlow<Boolean> = _carregandoEnderecosSalvos.asStateFlow()

    private val _enderecoSelecionadoId = MutableStateFlow<String?>(null)
    val enderecoSelecionadoId: StateFlow<String?> = _enderecoSelecionadoId.asStateFlow()

    private val _numero = MutableStateFlow("")
    val numero: StateFlow<String> = _numero.asStateFlow()

    private val _complemento = MutableStateFlow("")
    val complemento: StateFlow<String> = _complemento.asStateFlow()

    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState.asStateFlow()

    private val _avisoEstoque = MutableStateFlow<String?>(null)
    val avisoEstoque: StateFlow<String?> = _avisoEstoque.asStateFlow()

    private val _mensagemSucesso = MutableStateFlow<String?>(null)
    val mensagemSucesso: StateFlow<String?> = _mensagemSucesso.asStateFlow()

    fun limparMensagemSucesso() {
        _mensagemSucesso.value = null
    }

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

    fun adicionarVarios(mangas: List<Manga>) {
        mangas.forEach { adicionarItem(it) }
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

        persistirItem(manga.id, quantidadeAtualNoCarrinho + 1)
        _mensagemSucesso.value = "${manga.nome} adicionado ao carrinho!"
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
        persistirItem(manga.id, itemAtual.quantidade + 1)
    }

    fun diminuirQuantidade(manga: Manga) {
        val itemAtual = _itens.value.find { it.manga.id == manga.id } ?: return
        _itens.value = _itens.value.mapNotNull {
            if (it.manga.id == manga.id) {
                if (it.quantidade > 1) it.copy(quantidade = it.quantidade - 1) else null
            } else it
        }
        if (itemAtual.quantidade > 1) {
            persistirItem(manga.id, itemAtual.quantidade - 1)
        } else {
            persistirRemocaoItem(manga.id)
        }
    }

    fun removerItem(manga: Manga) {
        _itens.value = _itens.value.filterNot { it.manga.id == manga.id }
        persistirRemocaoItem(manga.id)
    }

    fun limparCarrinho() {
        _itens.value = emptyList()
        persistirLimpezaCarrinho()
    }

    fun definirUsuarioLogado(usuarioId: String) {
        if (usuarioId.isBlank() || usuarioId == usuarioIdAtual) return
        usuarioIdAtual = usuarioId

        viewModelScope.launch {
            val idCliente = clienteIdCache ?: usuarioRepository.buscarClienteId(usuarioId).getOrNull()
            clienteIdCache = idCliente
            if (idCliente == null) return@launch

            val itensDto = carrinhoRepository.listarItens(idCliente).getOrNull() ?: return@launch
            if (itensDto.isEmpty()) return@launch

            val todosMangas = mangaRepository.listarMangas().getOrNull() ?: return@launch
            val mangasPorId = todosMangas.associateBy { it.id }

            val itensRestaurados = itensDto.mapNotNull { itemDto ->
                val manga = mangasPorId[itemDto.produtoId] ?: return@mapNotNull null
                CartItem(manga = manga, quantidade = itemDto.quantidade.coerceAtMost(manga.estoque.coerceAtLeast(1)))
            }

            if (_itens.value.isEmpty() && itensRestaurados.isNotEmpty()) {
                _itens.value = itensRestaurados
            }
        }
    }

    private fun persistirItem(produtoId: String, quantidade: Int) {
        val uid = usuarioIdAtual ?: return
        viewModelScope.launch {
            val idCliente = clienteIdCache ?: usuarioRepository.buscarClienteId(uid).getOrNull() ?: return@launch
            clienteIdCache = idCliente
            carrinhoRepository.salvarItem(idCliente, produtoId, quantidade)
        }
    }

    private fun persistirRemocaoItem(produtoId: String) {
        val uid = usuarioIdAtual ?: return
        viewModelScope.launch {
            val idCliente = clienteIdCache ?: usuarioRepository.buscarClienteId(uid).getOrNull() ?: return@launch
            clienteIdCache = idCliente
            carrinhoRepository.removerItem(idCliente, produtoId)
        }
    }

    private fun persistirLimpezaCarrinho() {
        val uid = usuarioIdAtual ?: return
        viewModelScope.launch {
            val idCliente = clienteIdCache ?: usuarioRepository.buscarClienteId(uid).getOrNull() ?: return@launch
            clienteIdCache = idCliente
            carrinhoRepository.limparCarrinho(idCliente)
        }
    }

    fun atualizarCep(valor: String) {
        _enderecoSelecionadoId.value = null
        _cep.value = valor
        _frete.value = null
        _cepErro.value = null
        _cidadeUf.value = null
        _enderecoEncontrado.value = null
    }

    fun atualizarNumero(valor: String) {
        _numero.value = valor
    }

    fun atualizarComplemento(valor: String) {
        _complemento.value = valor
    }

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
                    _enderecoEncontrado.value = endereco
                    _cepErro.value = null
                },
                onFailure = { erro ->
                    _cepErro.value = erro.message ?: "Não foi possível calcular o frete."
                    _frete.value = null
                    _cidadeUf.value = null
                    _enderecoEncontrado.value = null
                }
            )
            _calculandoFrete.value = false
        }
    }

    private fun valorFretePorUf(uf: String): Double {
        val sudeste = setOf("SP", "RJ", "MG", "ES")
        val sulECentroOeste = setOf("PR", "SC", "RS", "MT", "MS", "GO", "DF")

        return when (uf.uppercase()) {
            in sudeste -> 12.0
            in sulECentroOeste -> 18.0
            else -> 25.0
        }
    }

    fun carregarEnderecosSalvos(userId: String) {
        if (userId.isBlank()) return

        _carregandoEnderecosSalvos.value = true
        viewModelScope.launch {
            val idCliente = clienteIdCache ?: usuarioRepository.buscarClienteId(userId).getOrNull()
            clienteIdCache = idCliente

            if (idCliente == null) {
                _carregandoEnderecosSalvos.value = false
                return@launch
            }

            val resultado = enderecoRepository.listarEnderecos(idCliente)
            resultado.onSuccess { lista ->
                _enderecosSalvos.value = lista
                if (_enderecoSelecionadoId.value == null && _cep.value.isBlank()) {
                    val sugestao = lista.firstOrNull { it.padrao } ?: lista.firstOrNull()
                    sugestao?.let { selecionarEnderecoSalvo(it) }
                }
            }
            _carregandoEnderecosSalvos.value = false
        }
    }

    fun selecionarEnderecoSalvo(endereco: Endereco) {
        _enderecoSelecionadoId.value = endereco.id
        _cep.value = endereco.cep
        _numero.value = endereco.numero
        _complemento.value = endereco.complemento
        _cidadeUf.value = "${endereco.cidade} - ${endereco.uf}"
        _frete.value = valorFretePorUf(endereco.uf)
        _cepErro.value = null
        _enderecoEncontrado.value = null
    }

    fun selecionarNovoEndereco() {
        _enderecoSelecionadoId.value = null
        _cep.value = ""
        _numero.value = ""
        _complemento.value = ""
        _cidadeUf.value = null
        _frete.value = null
        _cepErro.value = null
        _enderecoEncontrado.value = null
    }

    fun atualizarCupomInput(valor: String) {
        _cupomInput.value = valor
        _cupomErro.value = null
    }

    fun aplicarCupom(userId: String) {
        val codigo = _cupomInput.value.trim()

        if (codigo.isBlank()) {
            _cupomErro.value = "Informe um código de cupom."
            return
        }

        if (userId.isBlank()) {
            _cupomErro.value = "Não foi possível validar o cupom. Tente novamente."
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
                        _validandoCupom.value = false
                        return@launch
                    }

                    val usosPorUsuarioResult = cupomRepository.contarUsosPorUsuario(cupom.codigo, userId)
                    val usosPorUsuario = usosPorUsuarioResult.getOrElse {
                        _cupomErro.value = "Não foi possível validar o cupom. Tente novamente."
                        _cupomAplicado.value = null
                        _validandoCupom.value = false
                        return@launch
                    }
                    if (usosPorUsuario > 0) {
                        _cupomErro.value = "Você já utilizou este cupom anteriormente."
                        _cupomAplicado.value = null
                        _validandoCupom.value = false
                        return@launch
                    }

                    if (cupom.limiteTotal > 0) {
                        val usosTotaisResult = cupomRepository.contarUsosTotais(cupom.codigo)
                        val usosTotais = usosTotaisResult.getOrElse {
                            _cupomErro.value = "Não foi possível validar o cupom. Tente novamente."
                            _cupomAplicado.value = null
                            _validandoCupom.value = false
                            return@launch
                        }
                        if (usosTotais >= cupom.limiteTotal) {
                            _cupomErro.value = "Este cupom atingiu o limite de usos."
                            _cupomAplicado.value = null
                            _validandoCupom.value = false
                            return@launch
                        }
                    }

                    _cupomAplicado.value = cupom
                    _cupomErro.value = null
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

    fun finalizarCompra(userId: String, metodoPagamento: String) {
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
            delay(1500)
            val pagamentoAprovado = Math.random() > 0.2

            if (!pagamentoAprovado) {
                _checkoutState.value = CheckoutState.Erro(
                    "Pagamento via $metodoPagamento recusado. Verifique os dados e tente novamente."
                )
                return@launch
            }

            val produtoIds = itensAtuais.map { it.manga.id }
            val resultadoEstoque = mangaRepository.buscarEstoqueAtual(produtoIds)

            val estoqueAtualMap = resultadoEstoque.getOrElse {
                _checkoutState.value = CheckoutState.Erro("Não foi possível verificar o estoque. Tente novamente.")
                return@launch
            }

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

            val clienteId = clienteIdCache ?: usuarioRepository.buscarClienteId(userId).getOrNull()
            clienteIdCache = clienteId

            if (clienteId == null) {
                _checkoutState.value = CheckoutState.Erro(
                    "Não foi possível identificar seu cadastro de cliente. Tente sair e entrar novamente antes de finalizar a compra."
                )
                return@launch
            }

            var enderecoId: String? = _enderecoSelecionadoId.value
            val enderecoEncontrado = _enderecoEncontrado.value
            if (enderecoId == null && enderecoEncontrado != null) {
                enderecoId = enderecoRepository.salvarEndereco(
                    clienteId = clienteId,
                    cep = enderecoEncontrado.cep,
                    logradouro = enderecoEncontrado.logradouro,
                    numero = _numero.value,
                    complemento = _complemento.value,
                    bairro = enderecoEncontrado.bairro,
                    cidade = enderecoEncontrado.cidade,
                    uf = enderecoEncontrado.uf
                ).getOrNull()
            }

            if (enderecoId == null) {
                _checkoutState.value = CheckoutState.Erro(
                    "Selecione um endereço salvo ou informe um CEP válido antes de finalizar a compra."
                )
                return@launch
            }

            val subtotalAtual = itensAtuais.sumOf { it.subtotal }
            val cupomAtual = _cupomAplicado.value
            val descontoAtual = cupomAtual?.calcularDesconto(subtotalAtual) ?: 0.0

            val pedido = Order(
                userId = userId,
                clienteId = clienteId,
                enderecoId = enderecoId,
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
                    orderRepository.registrarPagamento(pedidoId, metodoPagamento, pedido.valorTotal)

                    _checkoutState.value = CheckoutState.Sucesso(pedidoId)
                    limparCarrinho()
                    _cep.value = ""
                    _frete.value = null
                    _cidadeUf.value = null
                    _numero.value = ""
                    _complemento.value = ""
                    _enderecoEncontrado.value = null
                    _enderecoSelecionadoId.value = null
                    removerCupom()
                },
                onFailure = { erro ->
                    _checkoutState.value = CheckoutState.Erro(erro.message ?: "Não foi possível finalizar a compra.")
                }
            )
        }
    }
}