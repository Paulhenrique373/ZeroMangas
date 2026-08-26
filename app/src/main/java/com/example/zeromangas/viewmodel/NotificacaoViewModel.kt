package com.example.zeromangas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zeromangas.data.model.Notificacao
import com.example.zeromangas.repository.NotificacaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

/**
 * Notificações do usuário (promoção, lançamento, pedido, favorito voltou ao
 * estoque/entrou em promoção). Carrega o histórico salvo e, enquanto o app
 * está aberto, escuta novas notificações em tempo real via Supabase Realtime
 * — sem precisar ficar consultando o banco em loop.
 */
class NotificacaoViewModel : ViewModel() {

    private val repository = NotificacaoRepository()

    private val _notificacoes = MutableStateFlow<List<Notificacao>>(emptyList())
    val notificacoes: StateFlow<List<Notificacao>> = _notificacoes.asStateFlow()

    val quantidadeNaoLidas: StateFlow<Int> = _notificacoes
        .map { lista -> lista.count { !it.lida } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var usuarioIdAtual: String? = null

    /**
     * Carrega o histórico e conecta o canal Realtime (uma vez só por usuário
     * — chamar de novo com o mesmo id não reconecta à toa).
     */
    fun iniciar(usuarioId: String) {
        if (usuarioId.isBlank() || usuarioId == usuarioIdAtual) return
        usuarioIdAtual = usuarioId

        viewModelScope.launch {
            repository.listarNotificacoes(usuarioId).onSuccess { _notificacoes.value = it }
        }

        viewModelScope.launch {
            repository.conectarCanal()
            repository.escutarNovasNotificacoes(usuarioId).collect { nova ->
                // Evita duplicar se, por algum motivo, a mesma notificação já estiver na lista
                // (ex: chegou pelo Realtime entre o carregamento inicial e a inscrição no canal).
                if (_notificacoes.value.none { it.id == nova.id }) {
                    _notificacoes.value = listOf(nova) + _notificacoes.value
                }
            }
        }
    }

    fun marcarComoLida(notificacaoId: String) {
        _notificacoes.value = _notificacoes.value.map {
            if (it.id == notificacaoId) it.copy(lida = true) else it
        }
        viewModelScope.launch { repository.marcarComoLida(notificacaoId) }
    }

    fun marcarTodasComoLidas() {
        val usuarioId = usuarioIdAtual ?: return
        _notificacoes.value = _notificacoes.value.map { it.copy(lida = true) }
        viewModelScope.launch { repository.marcarTodasComoLidas(usuarioId) }
    }

    fun limpar() {
        usuarioIdAtual = null
        _notificacoes.value = emptyList()
        viewModelScope.launch { repository.desconectarCanal() }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { repository.desconectarCanal() }
    }
}