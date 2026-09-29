package com.example.zeromangas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.data.remote.CategoriaDto
import com.example.zeromangas.data.remote.MarcaDto
import com.example.zeromangas.repository.AdminRepository
import com.example.zeromangas.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Estado do CRUD de Produtos do Painel Administrativo (item 2 do pedido original).
 * Fica hoisted aqui (e não como `remember` dentro da tela) justamente pra sobreviver
 * à navegação entre a lista e o formulário — mesmo padrão do AuthViewModel pro Perfil.
 */
class AdminViewModel(
    private val adminRepository: AdminRepository = AdminRepository(),
    private val mangaRepository: MangaRepository = MangaRepository()
) : ViewModel() {

    private val _produtos = MutableStateFlow<List<Manga>>(emptyList())
    val produtos: StateFlow<List<Manga>> = _produtos

    private val _carregandoProdutos = MutableStateFlow(false)
    val carregandoProdutos: StateFlow<Boolean> = _carregandoProdutos

    private val _erroProdutos = MutableStateFlow<String?>(null)
    val erroProdutos: StateFlow<String?> = _erroProdutos

    private val _categorias = MutableStateFlow<List<CategoriaDto>>(emptyList())
    val categorias: StateFlow<List<CategoriaDto>> = _categorias

    private val _marcas = MutableStateFlow<List<MarcaDto>>(emptyList())
    val marcas: StateFlow<List<MarcaDto>> = _marcas

    private val _carregandoCategoriasMarcas = MutableStateFlow(false)
    val carregandoCategoriasMarcas: StateFlow<Boolean> = _carregandoCategoriasMarcas

    /** Busca um produto já carregado na lista pelo id — usado pra abrir o formulário em modo edição. */
    fun produtoPorId(id: String): Manga? = _produtos.value.find { it.id == id }

    fun carregarProdutos() {
        viewModelScope.launch {
            _carregandoProdutos.value = true
            _erroProdutos.value = null
            mangaRepository.listarMangas(apenasAtivos = false)
                .onSuccess { _produtos.value = it }
                .onFailure { _erroProdutos.value = it.message ?: "Não foi possível carregar os produtos." }
            _carregandoProdutos.value = false
        }
    }

    /** Carrega as opções de marca/categoria pros dropdowns do formulário, se ainda não tiverem sido carregadas. */
    fun carregarOpcoesFormulario() {
        if (_categorias.value.isEmpty()) carregarCategorias()
        if (_marcas.value.isEmpty()) carregarMarcas()
    }

    /** Recarrega categorias do banco — usado tanto pelo formulário quanto pela tela de Categorias. */
    fun carregarCategorias() {
        viewModelScope.launch {
            _carregandoCategoriasMarcas.value = true
            mangaRepository.listarCategoriasCompletas().onSuccess { _categorias.value = it }
            _carregandoCategoriasMarcas.value = false
        }
    }

    /** Recarrega editoras do banco — usado tanto pelo formulário quanto pela tela de Editoras. */
    fun carregarMarcas() {
        viewModelScope.launch {
            _carregandoCategoriasMarcas.value = true
            mangaRepository.listarMarcasCompletas().onSuccess { _marcas.value = it }
            _carregandoCategoriasMarcas.value = false
        }
    }

    suspend fun salvarCategoria(id: String?, nome: String): Result<Unit> {
        val resultado = if (id == null) adminRepository.criarCategoria(nome).map { }
        else adminRepository.atualizarCategoria(id, nome)
        resultado.onSuccess { carregarCategorias() }
        return resultado
    }

    suspend fun salvarMarca(id: String?, nome: String): Result<Unit> {
        val resultado = if (id == null) adminRepository.criarMarca(nome).map { }
        else adminRepository.atualizarMarca(id, nome)
        resultado.onSuccess { carregarMarcas() }
        return resultado
    }

    suspend fun ajustarEstoque(idProduto: String, estoque: Int): Result<Unit> {
        val resultado = adminRepository.ajustarEstoque(idProduto, estoque)
        resultado.onSuccess { carregarProdutos() }
        return resultado
    }

    suspend fun salvarProduto(
        idProduto: String?,
        nome: String,
        marcaId: String,
        categoriaId: String,
        volume: Int,
        preco: Double,
        imagemUrl: String,
        descricao: String,
        estoque: Int,
        autor: String,
        emDestaque: Boolean
    ): Result<Unit> {
        val resultado = if (idProduto == null) {
            adminRepository.criarProduto(
                nome = nome, marcaId = marcaId, categoriaId = categoriaId, volume = volume,
                preco = preco, imagemUrl = imagemUrl, descricao = descricao, estoque = estoque,
                autor = autor, emDestaque = emDestaque
            ).map { }
        } else {
            adminRepository.atualizarProduto(
                idProduto = idProduto, nome = nome, marcaId = marcaId, categoriaId = categoriaId,
                volume = volume, preco = preco, imagemUrl = imagemUrl, descricao = descricao,
                estoque = estoque, autor = autor, emDestaque = emDestaque
            )
        }

        resultado.onSuccess { carregarProdutos() }
        return resultado
    }

    suspend fun alternarAtivo(idProduto: String, ativo: Boolean): Result<Unit> {
        val resultado = adminRepository.alternarAtivoProduto(idProduto, ativo)
        resultado.onSuccess { carregarProdutos() }
        return resultado
    }
}