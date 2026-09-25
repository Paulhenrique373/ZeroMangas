package com.example.zeromangas.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zeromangas.data.model.PerfilCliente
import com.example.zeromangas.data.model.User
import com.example.zeromangas.repository.StorageRepository
import com.example.zeromangas.repository.AdminRepository
import com.example.zeromangas.repository.AuthRepository
import com.example.zeromangas.repository.UsuarioRepository
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

sealed class UploadFotoState {
    object Idle : UploadFotoState()
    object Loading : UploadFotoState()
    data class Sucesso(val url: String) : UploadFotoState()
    data class Erro(val mensagem: String) : UploadFotoState()
}

sealed class PerfilCompletoState {
    object Idle : PerfilCompletoState()
    object Loading : PerfilCompletoState()
    object Sucesso : PerfilCompletoState()
    data class Erro(val mensagem: String) : PerfilCompletoState()
}

/**
 * Estado da troca de e-mail/senha. Antes de aplicar a mudança em si, pedimos
 * a senha atual e confirmamos com o Supabase Auth (ver [AuthRepository.reautenticar]).
 */
sealed class CredenciaisState {
    object Idle : CredenciaisState()
    object Loading : CredenciaisState()
    object EmailAlterado : CredenciaisState()
    object SenhaAlterada : CredenciaisState()
    data class Erro(val mensagem: String) : CredenciaisState()
}

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()
    private val storageRepository = StorageRepository()
    private val usuarioRepository = UsuarioRepository()
    private val adminRepository = AdminRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _usuarioAtual = MutableStateFlow<User?>(null)
    val usuarioAtual: StateFlow<User?> = _usuarioAtual

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState

    private val _uploadFotoState = MutableStateFlow<UploadFotoState>(UploadFotoState.Idle)
    val uploadFotoState: StateFlow<UploadFotoState> = _uploadFotoState

    private val _perfilCliente = MutableStateFlow<PerfilCliente?>(null)
    val perfilCliente: StateFlow<PerfilCliente?> = _perfilCliente

    private val _perfilCompletoState = MutableStateFlow<PerfilCompletoState>(PerfilCompletoState.Idle)
    val perfilCompletoState: StateFlow<PerfilCompletoState> = _perfilCompletoState

    private val _credenciaisState = MutableStateFlow<CredenciaisState>(CredenciaisState.Idle)
    val credenciaisState: StateFlow<CredenciaisState> = _credenciaisState

    // Nunca setado diretamente pra "true" por nenhum caller — só verificarAdmin()
    // escreve aqui, e sempre a partir da resposta do banco (ver AdminRepository).
    private val _souAdmin = MutableStateFlow(false)
    val souAdmin: StateFlow<Boolean> = _souAdmin

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
                onSuccess = { usuarioFirebase ->
                    // ALTERADO: antes o resultado de sincronizarUsuario era descartado — se a
                    // sincronização com a tabela "usuarios" falhasse (ex: erro de SQL na RPC),
                    // o app mostrava "cadastro concluído" mesmo assim, escondendo o problema.
                    // Agora, se falhar, avisamos e não seguimos como se tivesse dado certo.
                    val sincronizacao = usuarioRepository.sincronizarUsuario(usuarioFirebase.uid, nome, email)
                    sincronizacao.fold(
                        onSuccess = {
                            _authState.value = AuthState.Sucesso
                            carregarUsuario()
                            verificarAdmin()
                        },
                        onFailure = { erro ->
                            _authState.value = AuthState.Erro(
                                "Sua conta foi criada, mas não foi possível concluir o cadastro (${erro.message ?: "erro desconhecido"}). Tente fazer login novamente."
                            )
                        }
                    )
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
                onSuccess = { usuarioFirebase ->
                    val nome = usuarioFirebase.displayName ?: ""
                    // ALTERADO: mesmo motivo do cadastro — se sincronizarUsuario falhar aqui,
                    // o app seguia como se o login tivesse dado certo, mas o usuário nunca
                    // chegava a existir em "usuarios" (foi a causa do bug de "Usuário não
                    // encontrado" ao editar o perfil). Agora tratamos o erro explicitamente.
                    val sincronizacao = usuarioRepository.sincronizarUsuario(usuarioFirebase.uid, nome, email)
                    sincronizacao.fold(
                        onSuccess = {
                            _authState.value = AuthState.Sucesso
                            carregarUsuario()
                            verificarAdmin()
                        },
                        onFailure = { erro ->
                            _authState.value = AuthState.Erro(
                                "Login feito, mas não foi possível carregar seus dados (${erro.message ?: "erro desconhecido"}). Tente novamente."
                            )
                        }
                    )
                },
                onFailure = { erro -> _authState.value = AuthState.Erro(erro.message ?: "Erro ao fazer login") }
            )
        }
    }

    fun logout() {
        repository.logout()
        _authState.value = AuthState.Idle
        _usuarioAtual.value = null
        _souAdmin.value = false
    }

    fun resetarEstado() {
        _authState.value = AuthState.Idle
    }

    fun carregarUsuario() {
        _usuarioAtual.value = repository.obterUsuarioAtual()
    }

    /**
     * Confere no banco se o usuário logado tem o perfil "admin" (tabelas
     * perfis/usuario_perfil, via a função sou_admin()). Chamar depois de login/cadastro
     * bem-sucedidos e sempre que a tela de Perfil ou o Painel Admin forem abertos —
     * nunca cachear esse valor além da sessão do ViewModel.
     */
    fun verificarAdmin() {
        if (repository.currentUser == null) {
            _souAdmin.value = false
            return
        }
        viewModelScope.launch {
            val resultado = adminRepository.souAdmin()
            _souAdmin.value = resultado.getOrDefault(false)
        }
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

    fun uploadFotoPerfil(context: Context, uri: Uri) {
        val uid = repository.currentUser?.uid
        if (uid == null) {
            _uploadFotoState.value = UploadFotoState.Erro("Usuário não está logado")
            return
        }

        _uploadFotoState.value = UploadFotoState.Loading
        viewModelScope.launch {
            val resultado = storageRepository.uploadFotoPerfil(context, uri, uid)
            resultado.fold(
                onSuccess = { url -> _uploadFotoState.value = UploadFotoState.Sucesso(url) },
                onFailure = { erro -> _uploadFotoState.value = UploadFotoState.Erro(erro.message ?: "Erro ao enviar a foto") }
            )
        }
    }

    fun resetarUploadFotoState() {
        _uploadFotoState.value = UploadFotoState.Idle
    }

    /**
     * Carrega os dados estendidos do perfil (telefone, cpf, bio, gênero,
     * data de nascimento, foto salva) direto do Supabase. Chamar ao abrir
     * a tela de Editar Perfil.
     */
    fun carregarPerfilCompleto() {
        val uid = repository.currentUser?.uid ?: return

        viewModelScope.launch {
            val resultado = usuarioRepository.buscarPerfilCompleto(uid)
            resultado.fold(
                onSuccess = { perfil -> _perfilCliente.value = perfil },
                onFailure = { /* tela mostra os campos em branco; sem erro bloqueante aqui */ }
            )
        }
    }

    /**
     * Salva a edição de perfil inteira: nome e foto (Supabase Auth
     * user_metadata + tabela "clientes") e os campos que só existem no
     * Supabase (telefone, cpf, bio, gênero, nascimento).
     */
    fun salvarPerfilCompleto(
        nome: String,
        fotoUrl: String,
        telefone: String,
        cpf: String,
        bio: String,
        genero: String,
        dataNascimento: String
    ) {
        if (nome.isBlank()) {
            _perfilCompletoState.value = PerfilCompletoState.Erro("O nome não pode ficar em branco")
            return
        }

        val uid = repository.currentUser?.uid
        if (uid == null) {
            _perfilCompletoState.value = PerfilCompletoState.Erro("Usuário não está logado")
            return
        }

        _perfilCompletoState.value = PerfilCompletoState.Loading
        viewModelScope.launch {
            // Mantém o user_metadata do Supabase Auth (nome/foto exibidos no app)
            // em sincronia, mas quem manda pro checkout/pedidos/etc é sempre a
            // tabela "clientes".
            repository.atualizarPerfil(nome, fotoUrl)

            val resultado = usuarioRepository.atualizarPerfilCompleto(
                firebaseUid = uid,
                nome = nome,
                fotoUrl = fotoUrl.ifBlank { null },
                telefone = telefone.ifBlank { null },
                cpf = cpf.ifBlank { null },
                bio = bio.ifBlank { null },
                genero = genero.ifBlank { null },
                dataNascimento = dataNascimento.ifBlank { null }
            )

            resultado.fold(
                onSuccess = {
                    carregarUsuario()
                    carregarPerfilCompleto()
                    _perfilCompletoState.value = PerfilCompletoState.Sucesso
                },
                onFailure = { erro ->
                    _perfilCompletoState.value = PerfilCompletoState.Erro(erro.message ?: "Erro ao salvar perfil")
                }
            )
        }
    }

    fun resetarPerfilCompletoState() {
        _perfilCompletoState.value = PerfilCompletoState.Idle
    }

    /**
     * Troca o e-mail de login. O Supabase Auth manda um link de confirmação
     * pro e-mail NOVO — a troca só vale depois que o usuário clicar nesse
     * link, então avisa isso na tela em vez de tratar como "já trocado".
     */
    fun alterarEmail(senhaAtual: String, novoEmail: String) {
        if (senhaAtual.isBlank() || novoEmail.isBlank()) {
            _credenciaisState.value = CredenciaisState.Erro("Preencha a senha atual e o novo e-mail")
            return
        }

        _credenciaisState.value = CredenciaisState.Loading
        viewModelScope.launch {
            val reauth = repository.reautenticar(senhaAtual)
            if (reauth.isFailure) {
                _credenciaisState.value = CredenciaisState.Erro(
                    reauth.exceptionOrNull()?.message ?: "Senha atual incorreta"
                )
                return@launch
            }

            val resultado = repository.alterarEmail(novoEmail)
            resultado.fold(
                onSuccess = { _credenciaisState.value = CredenciaisState.EmailAlterado },
                onFailure = { erro ->
                    _credenciaisState.value = CredenciaisState.Erro(erro.message ?: "Erro ao trocar e-mail")
                }
            )
        }
    }

    /**
     * Troca a senha de login (exige confirmar a senha atual antes).
     */
    fun alterarSenha(senhaAtual: String, novaSenha: String) {
        if (senhaAtual.isBlank() || novaSenha.isBlank()) {
            _credenciaisState.value = CredenciaisState.Erro("Preencha a senha atual e a nova senha")
            return
        }
        if (novaSenha.length < 6) {
            _credenciaisState.value = CredenciaisState.Erro("A nova senha deve ter pelo menos 6 caracteres")
            return
        }

        _credenciaisState.value = CredenciaisState.Loading
        viewModelScope.launch {
            val reauth = repository.reautenticar(senhaAtual)
            if (reauth.isFailure) {
                _credenciaisState.value = CredenciaisState.Erro(
                    reauth.exceptionOrNull()?.message ?: "Senha atual incorreta"
                )
                return@launch
            }

            val resultado = repository.alterarSenha(novaSenha)
            resultado.fold(
                onSuccess = { _credenciaisState.value = CredenciaisState.SenhaAlterada },
                onFailure = { erro ->
                    _credenciaisState.value = CredenciaisState.Erro(erro.message ?: "Erro ao trocar senha")
                }
            )
        }
    }

    fun resetarCredenciaisState() {
        _credenciaisState.value = CredenciaisState.Idle
    }
}