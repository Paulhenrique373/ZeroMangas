package com.example.zeromangas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.repository.MangaRepository
import com.example.zeromangas.repository.PesquisaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

enum class TipoOrdenacao {
    NENHUMA, MENOR_PRECO, MAIOR_PRECO, A_Z, Z_A, MAIS_VENDIDOS, MAIS_RECENTES
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
    val autor: String?,
    val ordenacao: TipoOrdenacao,
    val faixaDePreco: Pair<Double?, Double?>
)

class HomeViewModel : ViewModel() {

    private val repository = MangaRepository()
    private val pesquisaRepository = PesquisaRepository()

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

    private val _autores = MutableStateFlow<List<String>>(emptyList())
    val autores: StateFlow<List<String>> = _autores.asStateFlow()

    private val _autorSelecionado = MutableStateFlow<String?>(null)
    val autorSelecionado: StateFlow<String?> = _autorSelecionado

    // ---- Histórico de pesquisas (Etapa 3, Parte 2) ----
    private val _pesquisasRecentes = MutableStateFlow<List<String>>(emptyList())
    val pesquisasRecentes: StateFlow<List<String>> = _pesquisasRecentes.asStateFlow()

    /**
     * Sugestões automáticas: até 5 mangás cujo nome bate com o texto digitado,
     * pra mostrar num dropdown enquanto o usuário ainda está digitando (antes
     * de "confirmar" a pesquisa). Não considera os outros filtros (categoria/
     * marca/preço) de propósito — é só uma prévia rápida por nome.
     */
    val sugestoes: StateFlow<List<Manga>> = combine(_todosMangas, _textoBusca) { todos, busca ->
        if (busca.isBlank()) {
            emptyList()
        } else {
            todos.filter { it.nome.contains(busca, ignoreCase = true) }.take(5)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _precoMinimo = MutableStateFlow<Double?>(null)
    val precoMinimo: StateFlow<Double?> = _precoMinimo

    private val _precoMaximo = MutableStateFlow<Double?>(null)
    val precoMaximo: StateFlow<Double?> = _precoMaximo

    private val _ordenacao = MutableStateFlow(TipoOrdenacao.NENHUMA)
    val ordenacao: StateFlow<TipoOrdenacao> = _ordenacao

    val mangasEmDestaque: StateFlow<List<Manga>> = _todosMangas
        .map { it.filter { manga -> manga.emDestaque } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * ETAPA 2 (Home): "🆕 Lançamentos" da Home.
     *
     * O model [Manga] ainda não tem uma coluna de data de criação no Supabase,
     * então não dá pra ordenar por "mais recente" de verdade sem inventar um dado
     * que não existe. Como heurística temporária e não-destrutiva, usamos os
     * últimos itens retornados pela listagem do catálogo (excluindo os que já
     * aparecem em "Mais vendidos") como aproximação de lançamentos.
     * Quando o banco ganhar uma coluna real (ex: "criado_em"), é só trocar o
     * "takeLast" por uma ordenação por essa data — o resto da Home não muda.
     */
    val mangasLancamentos: StateFlow<List<Manga>> = _todosMangas
        .map { lista ->
            lista.filterNot { it.emDestaque }.takeLast(10).reversed()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * ETAPA 2 (Home): "✨ Você também pode gostar".
     *
     * Sem um histórico de navegação ou preferências salvas do usuário, uma
     * recomendação "personalizada" de verdade ainda não é possível sem inventar
     * dados. Como aproximação razoável (mesmo padrão já usado em
     * DetalhesScreen/MangaRepository.buscarMangaComRecomendados), sugerimos
     * produtos da mesma categoria do mangá em destaque do banner.
     */
    val mangasRecomendados: StateFlow<List<Manga>> = combine(
        _todosMangas, mangasEmDestaque
    ) { todos, destaques ->
        val categoriaReferencia = destaques.firstOrNull()?.categoria
        if (categoriaReferencia.isNullOrBlank()) {
            emptyList()
        } else {
            todos.filter { it.categoria == categoriaReferencia && !it.emDestaque }.take(10)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combina min/max em um único fluxo para poder juntar com os demais filtros (combine tem limite de 5 fluxos)
    private val faixaDePreco = combine(_precoMinimo, _precoMaximo) { min, max -> min to max }

    // Agrupa busca/categoria/marca/autor primeiro (4 fluxos) e só depois junta com
    // ordenação + faixa de preço — combine só aceita até 5 fluxos por chamada.
    private data class FiltrosBasicos(val busca: String, val categoria: String?, val marca: String?, val autor: String?)

    private val filtrosBasicos = combine(
        _textoBusca, _categoriaSelecionada, _marcaSelecionada, _autorSelecionado
    ) { busca, categoria, marca, autor -> FiltrosBasicos(busca, categoria, marca, autor) }

    private val filtros: StateFlow<Filtros> = combine(
        filtrosBasicos, _ordenacao, faixaDePreco
    ) { fb, ordenacao, faixa ->
        Filtros(fb.busca, fb.categoria, fb.marca, fb.autor, ordenacao, faixa)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Filtros("", null, null, null, TipoOrdenacao.NENHUMA, null to null)
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

        if (f.autor != null) {
            resultado = resultado.filter { it.autor == f.autor }
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
            // ETAPA 4 (Busca): sem contagem real de vendas ainda, aproximamos "mais
            // vendidos" pelo mesmo sinal já usado na Home (manga.emDestaque).
            TipoOrdenacao.MAIS_VENDIDOS -> resultado.sortedByDescending { it.emDestaque }
            // ETAPA 4 (Busca): sem coluna de data de criação no banco, aproximamos
            // "mais recentes" pela ordem em que o catálogo foi retornado do Supabase
            // (mesma heurística do "🆕 Lançamentos" da Home, na ETAPA 2).
            TipoOrdenacao.MAIS_RECENTES -> {
                val ranking = todosMangas.withIndex().associate { (indice, manga) -> manga.id to indice }
                resultado.sortedByDescending { ranking[it.id] ?: -1 }
            }
            TipoOrdenacao.NENHUMA -> resultado
        }

        resultado
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val quantidadeFiltrosAtivos: StateFlow<Int> = combine(
        _categoriaSelecionada, _marcaSelecionada, _autorSelecionado, faixaDePreco
    ) { categoria, marca, autor, faixa ->
        var quantidade = 0
        if (categoria != null) quantidade++
        if (marca != null) quantidade++
        if (autor != null) quantidade++
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
            repository.listarAutores().onSuccess { _autores.value = it }

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

    fun selecionarAutor(autor: String?) {
        _autorSelecionado.value = if (_autorSelecionado.value == autor) null else autor
    }

    fun definirAutor(autor: String?) {
        _autorSelecionado.value = autor
    }

    fun carregarPesquisasRecentes(usuarioId: String) {
        if (usuarioId.isBlank()) return
        viewModelScope.launch {
            pesquisaRepository.listarPesquisasRecentes(usuarioId).onSuccess { _pesquisasRecentes.value = it }
        }
    }

    /** Registra o termo pesquisado no histórico (chamar quando o usuário "confirma" a busca). */
    fun registrarPesquisa(usuarioId: String) {
        val termo = _textoBusca.value.trim()
        if (usuarioId.isBlank() || termo.isBlank()) return

        viewModelScope.launch {
            pesquisaRepository.registrarPesquisa(usuarioId, termo).onSuccess {
                carregarPesquisasRecentes(usuarioId)
            }
        }
    }

    fun limparHistoricoPesquisas(usuarioId: String) {
        if (usuarioId.isBlank()) return
        viewModelScope.launch {
            pesquisaRepository.limparHistorico(usuarioId).onSuccess { _pesquisasRecentes.value = emptyList() }
        }
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
        _autorSelecionado.value = null
        _precoMinimo.value = null
        _precoMaximo.value = null
    }
}