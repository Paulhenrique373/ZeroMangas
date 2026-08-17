package com.example.zeromangas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

enum class TipoOrdenacao {
    NENHUMA, MENOR_PRECO, MAIOR_PRECO, A_Z, Z_A
}

/**
 * Agrupa os 5 filtros de texto/categoria/marca/ordenação/faixa em um único fluxo.
 * Necessário porque o "combine" do Kotlin só aceita até 5 flows por chamada,
 * e já precisamos combinar esse grupo com a lista de mangás vinda do Supabase.
 */
private data class Filtros(
    val busca: String,
    val categoria: String?,
    val marca: String?,
    val ordenacao: TipoOrdenacao,
    val faixaDePreco: Pair<Double?, Double?>
)

class HomeViewModel : ViewModel() {

    private val repository = MangaRepository()

    private val _todosMangas = MutableStateFlow<List<Manga>>(emptyList())

    private val _categorias = MutableStateFlow<List<String>>(emptyList())
    val categorias: StateFlow<List<String>> = _categorias.asStateFlow()

    private val _marcas = MutableStateFlow<List<String>>(emptyList())
    val marcas: StateFlow<List<String>> = _marcas.asStateFlow()

    private val _carregando = MutableStateFlow(true)
    val carregando: StateFlow<Boolean> = _carregando.asStateFlow()

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro.asStateFlow()

    private val _textoBusca = MutableStateFlow("")
    val textoBusca: StateFlow<String> = _textoBusca

    private val _categoriaSelecionada = MutableStateFlow<String?>(null)
    val categoriaSelecionada: StateFlow<String?> = _categoriaSelecionada

    private val _marcaSelecionada = MutableStateFlow<String?>(null)
    val marcaSelecionada: StateFlow<String?> = _marcaSelecionada

    private val _precoMinimo = MutableStateFlow<Double?>(null)
    val precoMinimo: StateFlow<Double?> = _precoMinimo

    private val _precoMaximo = MutableStateFlow<Double?>(null)
    val precoMaximo: StateFlow<Double?> = _precoMaximo

    private val _ordenacao = MutableStateFlow(TipoOrdenacao.NENHUMA)
    val ordenacao: StateFlow<TipoOrdenacao> = _ordenacao

    val mangasEmDestaque: StateFlow<List<Manga>> = _todosMangas
        .map { it.filter { manga -> manga.emDestaque } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combina min/max em um único fluxo para poder juntar com os demais filtros (combine tem limite de 5 fluxos)
    private val faixaDePreco = combine(_precoMinimo, _precoMaximo) { min, max -> min to max }

    private val filtros: StateFlow<Filtros> = combine(
        _textoBusca, _categoriaSelecionada, _marcaSelecionada, _ordenacao, faixaDePreco
    ) { busca, categoria, marca, ordenacao, faixa ->
        Filtros(busca, categoria, marca, ordenacao, faixa)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Filtros("", null, null, TipoOrdenacao.NENHUMA, null to null)
    )

    val mangasFiltrados: StateFlow<List<Manga>> = combine(
        _todosMangas, filtros
    ) { todosMangas, f ->
        var resultado = todosMangas

        if (f.busca.isNotBlank()) {
            resultado = resultado.filter {
                it.nome.contains(f.busca, ignoreCase = true) ||
                        it.marca.contains(f.busca, ignoreCase = true) ||
                        "volume ${it.volume}".contains(f.busca, ignoreCase = true) ||
                        it.volume.toString() == f.busca.trim()
            }
        }

        if (f.categoria != null) {
            resultado = resultado.filter { it.categoria == f.categoria }
        }

        if (f.marca != null) {
            resultado = resultado.filter { it.marca == f.marca }
        }

        val (precoMin, precoMax) = f.faixaDePreco
        if (precoMin != null) {
            resultado = resultado.filter { it.preco >= precoMin }
        }
        if (precoMax != null) {
            resultado = resultado.filter { it.preco <= precoMax }
        }

        resultado = when (f.ordenacao) {
            TipoOrdenacao.MENOR_PRECO -> resultado.sortedBy { it.preco }
            TipoOrdenacao.MAIOR_PRECO -> resultado.sortedByDescending { it.preco }
            TipoOrdenacao.A_Z -> resultado.sortedBy { it.nome }
            TipoOrdenacao.Z_A -> resultado.sortedByDescending { it.nome }
            TipoOrdenacao.NENHUMA -> resultado
        }

        resultado
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val quantidadeFiltrosAtivos: StateFlow<Int> = combine(
        _categoriaSelecionada, _marcaSelecionada, faixaDePreco
    ) { categoria, marca, faixa ->
        var quantidade = 0
        if (categoria != null) quantidade++
        if (marca != null) quantidade++
        if (faixa.first != null || faixa.second != null) quantidade++
        quantidade
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    init {
        carregarDados()
    }

    /**
     * Busca produtos, categorias e marcas do Supabase. Chamada na inicialização
     * e disponível como retry caso a primeira tentativa falhe (sem internet, etc).
     */
    fun carregarDados() {
        viewModelScope.launch {
            _carregando.value = true
            _erro.value = null

            val resultadoMangas = repository.listarMangas()
            resultadoMangas.fold(
                onSuccess = { _todosMangas.value = it },
                onFailure = { _erro.value = "Não foi possível carregar o catálogo. Verifique sua conexão." }
            )

            repository.listarCategorias().onSuccess { _categorias.value = it }
            repository.listarMarcas().onSuccess { _marcas.value = it }

            _carregando.value = false
        }
    }

    fun buscar(texto: String) {
        _textoBusca.value = texto
    }

    fun selecionarCategoria(categoria: String?) {
        _categoriaSelecionada.value = if (_categoriaSelecionada.value == categoria) null else categoria
    }

    fun definirCategoria(categoria: String?) {
        _categoriaSelecionada.value = categoria
    }

    fun selecionarMarca(marca: String?) {
        _marcaSelecionada.value = if (_marcaSelecionada.value == marca) null else marca
    }

    fun definirMarca(marca: String?) {
        _marcaSelecionada.value = marca
    }

    fun definirFaixaDePreco(min: Double?, max: Double?) {
        _precoMinimo.value = min
        _precoMaximo.value = max
    }

    fun ordenarPor(tipo: TipoOrdenacao) {
        _ordenacao.value = tipo
    }

    fun limparFiltros() {
        _categoriaSelecionada.value = null
        _marcaSelecionada.value = null
        _precoMinimo.value = null
        _precoMaximo.value = null
    }
}