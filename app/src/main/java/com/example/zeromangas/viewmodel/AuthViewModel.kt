package com.example.zeromangas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zeromangas.data.model.User
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

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    object Sucesso : ProfileState()
    data class Erro(val mensagem: String) : ProfileState()
}

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _usuarioAtual = MutableStateFlow<User?>(null)
    val usuarioAtual: StateFlow<User?> = _usuarioAtual

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState

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
                onSuccess = {
                    _authState.value = AuthState.Sucesso
                    carregarUsuario()
                },
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
                onSuccess = {
                    _authState.value = AuthState.Sucesso
                    carregarUsuario()
                },
                onFailure = { erro -> _authState.value = AuthState.Erro(erro.message ?: "Erro ao fazer login") }
            )
        }
    }

    fun logout() {
        repository.logout()
        _authState.value = AuthState.Idle
        _usuarioAtual.value = null
    }

    fun resetarEstado() {
        _authState.value = AuthState.Idle
    }

    fun carregarUsuario() {
        _usuarioAtual.value = repository.obterUsuarioAtual()
    }

    fun atualizarPerfil(nome: String, fotoUrl: String) {
        if (nome.isBlank()) {
            _profileState.value = ProfileState.Erro("O nome não pode ficar em branco")
            return
        }

        _profileState.value = ProfileState.Loading
        viewModelScope.launch {
            val resultado = repository.atualizarPerfil(nome, fotoUrl)
            resultado.fold(
                onSuccess = {
                    carregarUsuario()
                    _profileState.value = ProfileState.Sucesso
                },
                onFailure = { erro -> _profileState.value = ProfileState.Erro(erro.message ?: "Erro ao atualizar perfil") }
            )
        }
    }

    fun resetarProfileState() {
        _profileState.value = ProfileState.Idle
    }
}