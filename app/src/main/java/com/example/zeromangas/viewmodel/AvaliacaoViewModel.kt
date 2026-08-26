package com.example.zeromangas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zeromangas.data.model.Avaliacao
import com.example.zeromangas.repository.AvaliacaoRepository
import com.example.zeromangas.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AvaliacoesState {
    object Carregando : AvaliacoesState()
    data class Sucesso(val avaliacoes: List<Avaliacao>) : AvaliacoesState()
    data class Erro(val mensagem: String) : AvaliacoesState()
}

sealed class EnviarAvaliacaoState {
    object Idle : EnviarAvaliacaoState()
    object Enviando : EnviarAvaliacaoState()
    object Sucesso : EnviarAvaliacaoState()
    data class Erro(val mensagem: String) : EnviarAvaliacaoState()
}

/**
 * Avaliações (nota 1-5 + comentário) de um produto, usado na tela de Detalhes.
 * Resolve o cliente_id do usuário logado (mesmo padrão do [EnderecoViewModel])
 * antes de permitir enviar uma avaliação.
 */
class AvaliacaoViewModel : ViewModel() {

    private val avaliacaoRepository = AvaliacaoRepository()
    private val usuarioRepository = UsuarioRepository()

    private var clienteId: String? = null
    private var produtoIdCarregado: String? = null

    private val _avaliacoesState = MutableStateFlow<AvaliacoesState>(AvaliacoesState.Carregando)
    val avaliacoesState: StateFlow<AvaliacoesState> = _avaliacoesState.asStateFlow()

    private val _enviarState = MutableStateFlow<EnviarAvaliacaoState>(EnviarAvaliacaoState.Idle)
    val enviarState: StateFlow<EnviarAvaliacaoState> = _enviarState.asStateFlow()

    fun carregarAvaliacoes(produtoId: String) {
        produtoIdCarregado = produtoId
        _avaliacoesState.value = AvaliacoesState.Carregando
        viewModelScope.launch {
            val resultado = avaliacaoRepository.listarAvaliacoes(produtoId)
            resultado.fold(
                onSuccess = { lista -> _avaliacoesState.value = AvaliacoesState.Sucesso(lista) },
                onFailure = { erro -> _avaliacoesState.value = AvaliacoesState.Erro(erro.message ?: "Erro ao carregar avaliações") }
            )
        }
    }

    fun enviarAvaliacao(usuarioId: String, produtoId: String, nota: Int, comentario: String) {
        if (nota !in 1..5) {
            _enviarState.value = EnviarAvaliacaoState.Erro("Escolha de 1 a 5 estrelas")
            return
        }

        _enviarState.value = EnviarAvaliacaoState.Enviando
        viewModelScope.launch {
            val idCliente = clienteId ?: usuarioRepository.buscarClienteId(usuarioId).getOrNull()
            if (idCliente == null) {
                _enviarState.value = EnviarAvaliacaoState.Erro("Não foi possível identificar seu cadastro")
                return@launch
            }
            clienteId = idCliente

            val resultado = avaliacaoRepository.avaliarProduto(produtoId, idCliente, nota, comentario)
            resultado.fold(
                onSuccess = {
                    _enviarState.value = EnviarAvaliacaoState.Sucesso
                    carregarAvaliacoes(produtoId)
                },
                onFailure = { erro ->
                    _enviarState.value = EnviarAvaliacaoState.Erro(erro.message ?: "Erro ao enviar avaliação")
                }
            )
        }
    }

    fun resetarEnviarState() {
        _enviarState.value = EnviarAvaliacaoState.Idle
    }
}