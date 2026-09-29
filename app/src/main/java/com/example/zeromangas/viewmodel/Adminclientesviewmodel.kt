package com.example.zeromangas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zeromangas.repository.AdminClientesRepository
import com.example.zeromangas.repository.ClienteAdminDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Estado da tela de Clientes do Painel Administrativo. */
class AdminClientesViewModel(
    private val repository: AdminClientesRepository = AdminClientesRepository()
) : ViewModel() {

    private val _clientes = MutableStateFlow<List<ClienteAdminDto>>(emptyList())
    val clientes: StateFlow<List<ClienteAdminDto>> = _clientes

    private val _carregando = MutableStateFlow(false)
    val carregando: StateFlow<Boolean> = _carregando

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro

    fun carregar(busca: String = "") {
        viewModelScope.launch {
            _carregando.value = true
            _erro.value = null
            repository.listarClientes(busca)
                .onSuccess { _clientes.value = it }
                .onFailure { _erro.value = "Não foi possível carregar os clientes. Tente novamente." }
            _carregando.value = false
        }
    }
}