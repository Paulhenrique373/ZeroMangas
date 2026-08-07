package com.example.zeromangas.viewmodel

import androidx.lifecycle.ViewModel
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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

    private val _ordenacao = MutableStateFlow(TipoOrdenacao.NENHUMA)
    val ordenacao: StateFlow<TipoOrdenacao> = _ordenacao

    val mangasEmDestaque: List<Manga> = todosMangas.filter { it.emDestaque }

    val mangasFiltrados: StateFlow<List<Manga>> = combine(
        _textoBusca, _categoriaSelecionada, _marcaSelecionada, _ordenacao
    ) { busca, categoria, marca, ordenacao ->
        var resultado = todosMangas

        if (busca.isNotBlank()) {
            resultado = resultado.filter {
                it.nome.contains(busca, ignoreCase = true) ||
                        it.marca.contains(busca, ignoreCase = true) ||
                        "volume ${it.volume}".contains(busca, ignoreCase = true)
            }
        }

        if (categoria != null) {
            resultado = resultado.filter { it.categoria == categoria }
        }

        if (marca != null) {
            resultado = resultado.filter { it.marca == marca }
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

    fun buscar(texto: String) {
        _textoBusca.value = texto
    }

    fun selecionarCategoria(categoria: String?) {
        _categoriaSelecionada.value = if (_categoriaSelecionada.value == categoria) null else categoria
    }

    fun selecionarMarca(marca: String?) {
        _marcaSelecionada.value = if (_marcaSelecionada.value == marca) null else marca
    }

    fun ordenarPor(tipo: TipoOrdenacao) {
        _ordenacao.value = tipo
    }

    fun limparFiltros() {
        _textoBusca.value = ""
        _categoriaSelecionada.value = null
        _marcaSelecionada.value = null
        _ordenacao.value = TipoOrdenacao.NENHUMA
    }
}