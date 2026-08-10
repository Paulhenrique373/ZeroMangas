package com.example.zeromangas.viewmodel

import androidx.lifecycle.ViewModel
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted

enum class TipoOrdenacao {
    NENHUMA, MENOR_PRECO, MAIOR_PRECO, A_Z, Z_A
}

class HomeViewModel : ViewModel() {

    private val repository = MangaRepository()

    private val todosMangas = repository.listarMangas()
    val categorias = repository.listarCategorias()
    val marcas = repository.listarMarcas()

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

    val mangasEmDestaque: List<Manga> = todosMangas.filter { it.emDestaque }

    // Combina min/max em um único fluxo para poder juntar com os demais filtros (combine tem limite de 5 fluxos)
    private val faixaDePreco = combine(_precoMinimo, _precoMaximo) { min, max -> min to max }

    val mangasFiltrados: StateFlow<List<Manga>> = combine(
        _textoBusca, _categoriaSelecionada, _marcaSelecionada, _ordenacao, faixaDePreco
    ) { busca, categoria, marca, ordenacao, faixa ->
        var resultado = todosMangas

        if (busca.isNotBlank()) {
            resultado = resultado.filter {
                it.nome.contains(busca, ignoreCase = true) ||
                        it.marca.contains(busca, ignoreCase = true) ||
                        "volume ${it.volume}".contains(busca, ignoreCase = true) ||
                        it.volume.toString() == busca.trim()
            }
        }

        if (categoria != null) {
            resultado = resultado.filter { it.categoria == categoria }
        }

        if (marca != null) {
            resultado = resultado.filter { it.marca == marca }
        }

        val (precoMin, precoMax) = faixa
        if (precoMin != null) {
            resultado = resultado.filter { it.preco >= precoMin }
        }
        if (precoMax != null) {
            resultado = resultado.filter { it.preco <= precoMax }
        }

        resultado = when (ordenacao) {
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
        initialValue = todosMangas
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
