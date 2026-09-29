package com.example.zeromangas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zeromangas.repository.AdminPedidosRepository
import com.example.zeromangas.repository.ItemPedidoAdminDto
import com.example.zeromangas.repository.PedidoAdminDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Estado da tela de Pedidos do Painel Administrativo (item 5). */
class AdminPedidosViewModel(
    private val repository: AdminPedidosRepository = AdminPedidosRepository()
) : ViewModel() {

    private val _pedidos = MutableStateFlow<List<PedidoAdminDto>>(emptyList())
    val pedidos: StateFlow<List<PedidoAdminDto>> = _pedidos

    private val _carregando = MutableStateFlow(false)
    val carregando: StateFlow<Boolean> = _carregando

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro

    /** Status usado no filtro. Vazio = todos. */
    private val _filtroStatus = MutableStateFlow("")
    val filtroStatus: StateFlow<String> = _filtroStatus

    private val _itens = MutableStateFlow<List<ItemPedidoAdminDto>>(emptyList())
    val itens: StateFlow<List<ItemPedidoAdminDto>> = _itens

    private val _carregandoItens = MutableStateFlow(false)
    val carregandoItens: StateFlow<Boolean> = _carregandoItens

    private val _erroItens = MutableStateFlow<String?>(null)
    val erroItens: StateFlow<String?> = _erroItens

    fun carregar() {
        viewModelScope.launch {
            _carregando.value = true
            _erro.value = null
            repository.listarPedidos(_filtroStatus.value)
                .onSuccess { _pedidos.value = it }
                .onFailure { _erro.value = it.message ?: "Não foi possível carregar os pedidos." }
            _carregando.value = false
        }
    }

    fun selecionarFiltro(status: String) {
        if (_filtroStatus.value == status) return
        _filtroStatus.value = status
        carregar()
    }

    fun carregarItens(idPedido: String) {
        viewModelScope.launch {
            _carregandoItens.value = true
            _erroItens.value = null
            _itens.value = emptyList()
            repository.listarItens(idPedido)
                .onSuccess { _itens.value = it }
                .onFailure { _erroItens.value = it.message ?: "Não foi possível carregar os itens." }
            _carregandoItens.value = false
        }
    }

    suspend fun avancarStatus(idPedido: String, novoStatus: String): Result<Unit> {
        val resultado = repository.mudarStatus(idPedido, novoStatus)
        resultado.onSuccess { carregar() }
        return resultado
    }

    suspend fun cancelar(idPedido: String): Result<Unit> {
        val resultado = repository.cancelarPedido(idPedido)
        resultado.onSuccess { carregar() }
        return resultado
    }
}