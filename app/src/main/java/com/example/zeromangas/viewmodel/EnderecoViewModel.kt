package com.example.zeromangas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zeromangas.data.model.Endereco
import com.example.zeromangas.repository.AuthRepository
import com.example.zeromangas.repository.EnderecoRepository
import com.example.zeromangas.repository.ViaCepRepository
import com.example.zeromangas.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class EnderecosState {
    object Carregando : EnderecosState()
    data class Sucesso(val enderecos: List<Endereco>) : EnderecosState()
    data class Erro(val mensagem: String) : EnderecosState()
}

sealed class SalvarEnderecoState {
    object Idle : SalvarEnderecoState()
    object Salvando : SalvarEnderecoState()
    object Sucesso : SalvarEnderecoState()
    data class Erro(val mensagem: String) : SalvarEnderecoState()
}

sealed class BuscaCepState {
    object Idle : BuscaCepState()
    object Buscando : BuscaCepState()
    object Encontrado : BuscaCepState()
    data class Erro(val mensagem: String) : BuscaCepState()
}

/**
 * Tela "Meus Endereços": lista, cria, edita, exclui e define o endereço
 * padrão do cliente logado. O [clienteId] é resolvido uma vez (a partir do
 * uid do Supabase Auth) e reaproveitado nas chamadas seguintes, mesmo padrão
 * já usado no checkout ([com.example.zeromangas.viewmodel.CartViewModel]).
 */
class EnderecoViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val usuarioRepository = UsuarioRepository()
    private val enderecoRepository = EnderecoRepository()
    private val viaCepRepository = ViaCepRepository()

    private var clienteId: String? = null

    private val _enderecosState = MutableStateFlow<EnderecosState>(EnderecosState.Carregando)
    val enderecosState: StateFlow<EnderecosState> = _enderecosState.asStateFlow()

    private val _salvarState = MutableStateFlow<SalvarEnderecoState>(SalvarEnderecoState.Idle)
    val salvarState: StateFlow<SalvarEnderecoState> = _salvarState.asStateFlow()

    private val _buscaCepState = MutableStateFlow<BuscaCepState>(BuscaCepState.Idle)
    val buscaCepState: StateFlow<BuscaCepState> = _buscaCepState.asStateFlow()

    private val _cepPreenchido = MutableStateFlow<com.example.zeromangas.repository.EnderecoCep?>(null)
    val cepPreenchido: StateFlow<com.example.zeromangas.repository.EnderecoCep?> = _cepPreenchido.asStateFlow()

    /**
     * Carrega os endereços do cliente logado. Resolve o cliente_id primeiro
     * (uma vez só) e depois lista.
     */
    fun carregarEnderecos() {
        val uid = authRepository.currentUser?.uid
        if (uid == null) {
            _enderecosState.value = EnderecosState.Erro("Você precisa estar logado")
            return
        }

        _enderecosState.value = EnderecosState.Carregando
        viewModelScope.launch {
            val idResolvido = clienteId ?: usuarioRepository.buscarClienteId(uid).getOrNull()

            if (idResolvido == null) {
                _enderecosState.value = EnderecosState.Erro("Não foi possível identificar seu cadastro")
                return@launch
            }
            clienteId = idResolvido

            val resultado = enderecoRepository.listarEnderecos(idResolvido)
            resultado.fold(
                onSuccess = { lista -> _enderecosState.value = EnderecosState.Sucesso(lista) },
                onFailure = { erro ->
                    _enderecosState.value = EnderecosState.Erro(erro.message ?: "Erro ao carregar endereços")
                }
            )
        }
    }

    /**
     * Consulta o CEP na API ViaCEP e guarda o resultado em [cepPreenchido],
     * pra tela preencher Estado/Cidade/Bairro/Rua automaticamente.
     */
    fun buscarCep(cep: String) {
        val cepLimpo = cep.filter { it.isDigit() }
        if (cepLimpo.length != 8) {
            _buscaCepState.value = BuscaCepState.Erro("CEP inválido")
            return
        }

        _buscaCepState.value = BuscaCepState.Buscando
        viewModelScope.launch {
            val resultado = viaCepRepository.buscarEndereco(cepLimpo)
            resultado.fold(
                onSuccess = { endereco ->
                    _cepPreenchido.value = endereco
                    _buscaCepState.value = BuscaCepState.Encontrado
                },
                onFailure = { erro ->
                    _buscaCepState.value = BuscaCepState.Erro(erro.message ?: "CEP não encontrado")
                }
            )
        }
    }

    fun resetarBuscaCep() {
        _buscaCepState.value = BuscaCepState.Idle
        _cepPreenchido.value = null
    }

    /**
     * Cria um endereço novo. [padrao] força esse endereço como padrão mesmo
     * já existindo outros (o banco cuida de tirar o padrão dos demais).
     */
    fun adicionarEndereco(
        nomeDestinatario: String,
        telefone: String,
        cep: String,
        uf: String,
        cidade: String,
        bairro: String,
        logradouro: String,
        numero: String,
        complemento: String,
        informacoesAdicionais: String,
        padrao: Boolean
    ) {
        val idCliente = clienteId
        if (idCliente == null) {
            _salvarState.value = SalvarEnderecoState.Erro("Não foi possível identificar seu cadastro")
            return
        }
        if (nomeDestinatario.isBlank() || cep.isBlank() || logradouro.isBlank() || numero.isBlank()) {
            _salvarState.value = SalvarEnderecoState.Erro("Preencha destinatário, CEP, rua e número")
            return
        }

        _salvarState.value = SalvarEnderecoState.Salvando
        viewModelScope.launch {
            val resultado = enderecoRepository.salvarEndereco(
                clienteId = idCliente,
                cep = cep,
                logradouro = logradouro,
                numero = numero,
                complemento = complemento,
                bairro = bairro,
                cidade = cidade,
                uf = uf,
                nomeDestinatario = nomeDestinatario,
                telefone = telefone,
                informacoesAdicionais = informacoesAdicionais,
                padrao = padrao
            )
            resultado.fold(
                onSuccess = {
                    _salvarState.value = SalvarEnderecoState.Sucesso
                    carregarEnderecos()
                },
                onFailure = { erro ->
                    _salvarState.value = SalvarEnderecoState.Erro(erro.message ?: "Erro ao salvar endereço")
                }
            )
        }
    }

    fun editarEndereco(
        enderecoId: String,
        nomeDestinatario: String,
        telefone: String,
        cep: String,
        uf: String,
        cidade: String,
        bairro: String,
        logradouro: String,
        numero: String,
        complemento: String,
        informacoesAdicionais: String
    ) {
        val idCliente = clienteId
        if (idCliente == null) {
            _salvarState.value = SalvarEnderecoState.Erro("Não foi possível identificar seu cadastro")
            return
        }
        if (nomeDestinatario.isBlank() || cep.isBlank() || logradouro.isBlank() || numero.isBlank()) {
            _salvarState.value = SalvarEnderecoState.Erro("Preencha destinatário, CEP, rua e número")
            return
        }

        _salvarState.value = SalvarEnderecoState.Salvando
        viewModelScope.launch {
            val resultado = enderecoRepository.editarEndereco(
                enderecoId = enderecoId,
                clienteId = idCliente,
                cep = cep,
                logradouro = logradouro,
                numero = numero,
                complemento = complemento,
                bairro = bairro,
                cidade = cidade,
                uf = uf,
                nomeDestinatario = nomeDestinatario,
                telefone = telefone,
                informacoesAdicionais = informacoesAdicionais
            )
            resultado.fold(
                onSuccess = {
                    _salvarState.value = SalvarEnderecoState.Sucesso
                    carregarEnderecos()
                },
                onFailure = { erro ->
                    _salvarState.value = SalvarEnderecoState.Erro(erro.message ?: "Erro ao atualizar endereço")
                }
            )
        }
    }

    fun excluirEndereco(enderecoId: String) {
        val idCliente = clienteId ?: return
        viewModelScope.launch {
            enderecoRepository.excluirEndereco(enderecoId, idCliente)
            carregarEnderecos()
        }
    }

    fun definirComoPadrao(enderecoId: String) {
        val idCliente = clienteId ?: return
        viewModelScope.launch {
            enderecoRepository.definirEnderecoPadrao(enderecoId, idCliente)
            carregarEnderecos()
        }
    }

    fun resetarSalvarState() {
        _salvarState.value = SalvarEnderecoState.Idle
    }
}