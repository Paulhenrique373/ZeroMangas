package com.example.zeromangas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.repository.FavoritoRepository
import com.example.zeromangas.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritoViewModel : ViewModel() {

    private val repository = FavoritoRepository()
    private val mangaRepository = MangaRepository()

    private val _favoritosIds = MutableStateFlow<Set<String>>(emptySet())
    val favoritosIds: StateFlow<Set<String>> = _favoritosIds.asStateFlow()

    /**
     * Lista de mangás favoritados (já convertida de ids para objetos Manga completos),
     * pronta para ser exibida na tela de Favoritos.
     */
    val mangasFavoritos: StateFlow<List<Manga>> = _favoritosIds
        .map { ids -> mangaRepository.listarMangas().filter { it.id in ids } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var usuarioIdCarregado: String? = null

    fun carregarFavoritos(usuarioId: String) {
        if (usuarioId.isBlank() || usuarioId == usuarioIdCarregado) return
        usuarioIdCarregado = usuarioId

        viewModelScope.launch {
            repository.listarFavoritos(usuarioId).onSuccess { ids ->
                _favoritosIds.value = ids.toSet()
            }
        }
    }

    fun isFavorito(mangaId: String): Boolean = mangaId in _favoritosIds.value

    /**
     * Adiciona/remove o favorito com atualização otimista (a UI reage na hora),
     * e desfaz a mudança se a operação no Firestore falhar.
     */
    fun alternarFavorito(usuarioId: String, manga: Manga) {
        if (usuarioId.isBlank()) return

        val jaEraFavorito = manga.id in _favoritosIds.value
        _favoritosIds.value = if (jaEraFavorito) {
            _favoritosIds.value - manga.id
        } else {
            _favoritosIds.value + manga.id
        }

        viewModelScope.launch {
            val resultado = if (jaEraFavorito) {
                repository.removerFavorito(usuarioId, manga.id)
            } else {
                repository.adicionarFavorito(usuarioId, manga.id)
            }

            resultado.onFailure {
                // Reverte em caso de erro de rede/permissão
                _favoritosIds.value = if (jaEraFavorito) {
                    _favoritosIds.value + manga.id
                } else {
                    _favoritosIds.value - manga.id
                }
            }
        }
    }

    fun limparFavoritos() {
        _favoritosIds.value = emptySet()
        usuarioIdCarregado = null
    }
}