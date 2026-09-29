package com.example.zeromangas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zeromangas.repository.AdminCuponsRepository
import com.example.zeromangas.repository.CupomAdminDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Estado da tela de Cupons do Painel Administrativo. */
class AdminCuponsViewModel(
    private val repository: AdminCuponsRepository = AdminCuponsRepository()
) : ViewModel() {

    private val _cupons = MutableStateFlow<List<CupomAdminDto>>(emptyList())
    val cupons: StateFlow<List<CupomAdminDto>> = _cupons

    private val _carregando = MutableStateFlow(false)
    val carregando: StateFlow<Boolean> = _carregando

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro

    fun carregar() {
        viewModelScope.launch {
            _carregando.value = true
            _erro.value = null
            repository.listarCupons()
                .onSuccess { _cupons.value = it }
                .onFailure { _erro.value = "Não foi possível carregar os cupons. Tente novamente." }
            _carregando.value = false
        }
    }

    suspend fun criar(
        codigo: String,
        tipoDesconto: String,
        valor: Double,
        valorMinimo: Double,
        limiteTotal: Int
    ): Result<Unit> {
        val r = repository.criarCupom(codigo, tipoDesconto, valor, valorMinimo, limiteTotal)
        r.onSuccess { carregar() }
        return r
    }

    suspend fun atualizar(
        id: String,
        tipoDesconto: String,
        valor: Double,
        valorMinimo: Double,
        limiteTotal: Int
    ): Result<Unit> {
        val r = repository.atualizarCupom(id, tipoDesconto, valor, valorMinimo, limiteTotal)
        r.onSuccess { carregar() }
        return r
    }

    suspend fun alternarAtivo(id: String, ativo: Boolean): Result<Unit> {
        val r = repository.alternarAtivo(id, ativo)
        r.onSuccess { carregar() }
        return r
    }
}