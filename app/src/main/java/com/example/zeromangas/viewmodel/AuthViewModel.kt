package com.example.zeromangas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zeromangas.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Sucesso : AuthState()
    data class Erro(val mensagem: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    val usuarioLogado get() = repository.currentUser != null

    fun cadastrar(nome: String, email: String, senha: String) {
        if (nome.isBlank() || email.isBlank() || senha.isBlank()) {
            _authState.value = AuthState.Erro("Preencha todos os campos")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val resultado = repository.cadastrar(nome, email, senha)
            resultado.fold(
                onSuccess = { _authState.value = AuthState.Sucesso },
                onFailure = { erro -> _authState.value = AuthState.Erro(erro.message ?: "Erro ao cadastrar") }
            )
        }
    }

    fun login(email: String, senha: String) {
        if (email.isBlank() || senha.isBlank()) {
            _authState.value = AuthState.Erro("Preencha todos os campos")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val resultado = repository.login(email, senha)
            resultado.fold(
                onSuccess = { _authState.value = AuthState.Sucesso },
                onFailure = { erro -> _authState.value = AuthState.Erro(erro.message ?: "Erro ao fazer login") }
            )
        }
    }

    fun logout() {
        repository.logout()
        _authState.value = AuthState.Idle
    }

    fun resetarEstado() {
        _authState.value = AuthState.Idle
    }
}