package com.example.zeromangas.repository

import com.example.zeromangas.data.model.User
import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException

/**
 * Usuário autenticado no Supabase Auth. Espelha a mesma superfície que o
 * antigo FirebaseUser oferecia (uid/displayName/email/photoUrl) — assim
 * [AuthViewModel], [EnderecoViewModel] e o [com.example.zeromangas.navigation.NavGraph]
 * continuam lendo ".uid" sem precisar mudar nada.
 */
data class SupabaseUsuario(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?
)

/**
 * Repositório de autenticação. Antes usava Firebase Auth; agora fala 100% com
 * o Supabase Auth (módulo "Auth" do supabase-kt). Nome, foto e demais dados
 * de perfil ficam salvos no "user_metadata" do próprio usuário no Supabase
 * (chaves "nome" e "foto_url"), já que o Supabase Auth não tem campos nativos
 * de displayName/photoUrl como o Firebase tinha.
 */
class AuthRepository {

    private val auth = SupabaseClient.client.auth

    // Escopo próprio só pra poder chamar o signOut() (que é suspend na API do
    // Supabase) de dentro de logout(), que continua uma função comum — os
    // callers atuais (AuthViewModel, NavGraph) chamam logout() direto, fora
    // de uma coroutine, então não dá pra tornar essa função suspend sem
    // mexer em todo mundo que a chama.
    private val escopoAuxiliar = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val currentUser: SupabaseUsuario?
        get() = auth.currentUserOrNull()?.paraSupabaseUsuario()

    suspend fun cadastrar(nome: String, email: String, senha: String): Result<SupabaseUsuario> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                password = senha
                data = buildJsonObject {
                    put("nome", JsonPrimitive(nome))
                }
            }

            // Se "Confirm email" estiver ligado no projeto Supabase (Auth > Providers),
            // o cadastro fica pendente de confirmação por link e nenhuma sessão é
            // criada ainda — nesse caso avisamos em vez de seguir como se já
            // estivesse logado.
            val usuario = auth.currentUserOrNull()
                ?: return Result.failure(
                    Exception("Cadastro realizado! Confirme seu e-mail antes de entrar.")
                )

            Result.success(usuario.paraSupabaseUsuario(nomeFallback = nome))
        } catch (e: Exception) {
            Result.failure(Exception(traduzirErroSupabase(e)))
        }
    }

    suspend fun login(email: String, senha: String): Result<SupabaseUsuario> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                password = senha
            }

            val usuario = auth.currentUserOrNull()
                ?: return Result.failure(Exception("Erro ao fazer login"))

            Result.success(usuario.paraSupabaseUsuario())
        } catch (e: Exception) {
            Result.failure(Exception(traduzirErroSupabase(e)))
        }
    }

    /**
     * Continua não-suspend de propósito (mesma assinatura de antes), já que
     * quem chama (AuthViewModel.logout(), acionado direto do onClick nas
     * telas) não está dentro de uma coroutine. O signOut() de verdade roda
     * em background; a tela não depende do resultado porque já limpa o
     * estado local (usuarioAtual, carrinho, favoritos) e navega pra Login
     * na hora, de qualquer forma.
     */
    fun logout() {
        escopoAuxiliar.launch {
            try {
                auth.signOut()
            } catch (_: Exception) {
                // Ex.: sem internet ao sair — não trava o app, a sessão local
                // do dispositivo já é descartada mesmo se a chamada de rede falhar.
            }
        }
    }

    fun obterUsuarioAtual(): User? {
        val usuario = auth.currentUserOrNull() ?: return null
        val metadata = usuario.userMetadata
        return User(
            uid = usuario.id,
            nome = metadata?.get("nome")?.jsonPrimitive?.contentOrNull ?: "",
            email = usuario.email ?: "",
            fotoUrl = metadata?.get("foto_url")?.jsonPrimitive?.contentOrNull ?: ""
        )
    }

    suspend fun atualizarPerfil(nome: String, fotoUrl: String): Result<Unit> {
        return try {
            auth.updateUser {
                data = buildJsonObject {
                    put("nome", JsonPrimitive(nome))
                    put("foto_url", JsonPrimitive(fotoUrl))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(traduzirErroSupabase(e)))
        }
    }

    /**
     * "Reautentica" confirmando a senha atual antes de trocar e-mail ou senha.
     * O Supabase Auth não exige isso como o Firebase fazia (não há erro de
     * "login recente exigido"), mas mantemos essa confirmação por segurança —
     * evita que alguém com o celular desbloqueado troque as credenciais sem
     * saber a senha atual. Reaproveita o próprio signInWith pra validar.
     */
    suspend fun reautenticar(senhaAtual: String): Result<Unit> {
        return try {
            val email = auth.currentUserOrNull()?.email
                ?: return Result.failure(Exception("Usuário sem e-mail cadastrado"))

            auth.signInWith(Email) {
                this.email = email
                password = senhaAtual
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Senha atual incorreta."))
        }
    }

    /**
     * Troca o e-mail de login. O Supabase manda e-mail de confirmação pro
     * endereço novo — a troca só vale depois que o usuário clicar no link.
     * Chame [reautenticar] antes, com a senha atual.
     */
    suspend fun alterarEmail(novoEmail: String): Result<Unit> {
        return try {
            auth.updateUser {
                email = novoEmail
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(traduzirErroSupabase(e)))
        }
    }

    /**
     * Troca a senha de login. Chame [reautenticar] antes, com a senha atual.
     */
    suspend fun alterarSenha(novaSenha: String): Result<Unit> {
        return try {
            auth.updateUser {
                password = novaSenha
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(traduzirErroSupabase(e)))
        }
    }

    /**
     * Envia o e-mail de recuperação de senha (link de redefinição) pro
     * endereço informado. Pronta pra usar num futuro "Esqueci minha senha"
     * na tela de Login (ainda não existe UI pra isso no app).
     */
    suspend fun enviarRecuperacaoSenha(email: String): Result<Unit> {
        return try {
            auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(traduzirErroSupabase(e)))
        }
    }

    private fun UserInfo.paraSupabaseUsuario(nomeFallback: String? = null): SupabaseUsuario {
        val metadata = userMetadata
        return SupabaseUsuario(
            uid = id,
            displayName = metadata?.get("nome")?.jsonPrimitive?.contentOrNull ?: nomeFallback,
            email = email,
            photoUrl = metadata?.get("foto_url")?.jsonPrimitive?.contentOrNull
        )
    }

    /**
     * Converte as exceptions do Supabase Auth (mensagens em inglês do SDK) em
     * mensagens em português, prontas para mostrar na tela.
     */
    private fun traduzirErroSupabase(e: Exception): String {
        val mensagem = e.message ?: ""
        return when {
            mensagem.contains("Invalid login credentials", ignoreCase = true) ->
                "E-mail ou senha inválidos."

            mensagem.contains("already registered", ignoreCase = true) ||
                    mensagem.contains("already exists", ignoreCase = true) ->
                "Este e-mail já está cadastrado. Tente fazer login."

            mensagem.contains("Password should be at least", ignoreCase = true) ->
                "A senha deve ter pelo menos 6 caracteres."

            mensagem.contains("email", ignoreCase = true) && mensagem.contains("invalid", ignoreCase = true) ->
                "E-mail inválido."

            e is IOException || mensagem.contains("network", ignoreCase = true) ->
                "Sem conexão com a internet. Verifique sua rede e tente novamente."

            else ->
                "Ocorreu um erro inesperado: $mensagem"
        }
    }
}