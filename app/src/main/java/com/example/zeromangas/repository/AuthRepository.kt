package com.example.zeromangas.repository

import android.net.Uri
import com.example.zeromangas.data.model.User
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun cadastrar(nome: String, email: String, senha: String): Result<FirebaseUser> {
        return try {
            val resultado = auth.createUserWithEmailAndPassword(email, senha).await()
            val usuario = resultado.user

            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(nome)
                .build()
            usuario?.updateProfile(profileUpdate)?.await()

            if (usuario != null) {
                Result.success(usuario)
            } else {
                Result.failure(Exception("Erro ao criar usuário"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(traduzirErroFirebase(e)))
        }
    }

    suspend fun login(email: String, senha: String): Result<FirebaseUser> {
        return try {
            val resultado = auth.signInWithEmailAndPassword(email, senha).await()
            val usuario = resultado.user
            if (usuario != null) {
                Result.success(usuario)
            } else {
                Result.failure(Exception("Erro ao fazer login"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(traduzirErroFirebase(e)))
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun obterUsuarioAtual(): User? {
        val usuario = auth.currentUser ?: return null
        return User(
            uid = usuario.uid,
            nome = usuario.displayName ?: "",
            email = usuario.email ?: "",
            fotoUrl = usuario.photoUrl?.toString() ?: ""
        )
    }

    suspend fun atualizarPerfil(nome: String, fotoUrl: String): Result<Unit> {
        return try {
            val usuario = auth.currentUser ?: return Result.failure(Exception("Usuário não encontrado"))

            val builder = UserProfileChangeRequest.Builder()
                .setDisplayName(nome)

            if (fotoUrl.isNotBlank()) {
                builder.setPhotoUri(Uri.parse(fotoUrl))
            } else {
                builder.setPhotoUri(null)
            }

            usuario.updateProfile(builder.build()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(traduzirErroFirebase(e)))
        }
    }

    /**
     * Reautentica o usuário com a senha atual. O Firebase exige isso pouco antes
     * de trocar e-mail ou senha "por segurança" (não deixa fazer direto se o
     * login foi há muito tempo) — então essa é sempre a primeira chamada antes
     * de [alterarEmail] ou [alterarSenha].
     */
    suspend fun reautenticar(senhaAtual: String): Result<Unit> {
        return try {
            val usuario = auth.currentUser
                ?: return Result.failure(Exception("Usuário não encontrado"))
            val email = usuario.email
                ?: return Result.failure(Exception("Usuário sem e-mail cadastrado"))

            val credencial = EmailAuthProvider.getCredential(email, senhaAtual)
            usuario.reauthenticate(credencial).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(traduzirErroFirebase(e)))
        }
    }

    /**
     * Troca o e-mail de login no Firebase Auth. Chame [reautenticar] antes,
     * com a senha atual, ou isso pode falhar com "login recente exigido".
     */
    suspend fun alterarEmail(novoEmail: String): Result<Unit> {
        return try {
            val usuario = auth.currentUser ?: return Result.failure(Exception("Usuário não encontrado"))
            usuario.verifyBeforeUpdateEmail(novoEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(traduzirErroFirebase(e)))
        }
    }

    /**
     * Troca a senha no Firebase Auth. Chame [reautenticar] antes, com a senha
     * atual, ou isso pode falhar com "login recente exigido".
     */
    suspend fun alterarSenha(novaSenha: String): Result<Unit> {
        return try {
            val usuario = auth.currentUser ?: return Result.failure(Exception("Usuário não encontrado"))
            usuario.updatePassword(novaSenha).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(traduzirErroFirebase(e)))
        }
    }

    /**
     * Converte as exceptions específicas do Firebase Auth (que vêm com mensagens
     * em inglês do SDK) em mensagens em português, prontas para mostrar na tela.
     */
    private fun traduzirErroFirebase(e: Exception): String {
        return when (e) {
            is FirebaseAuthWeakPasswordException ->
                "A senha deve ter pelo menos 6 caracteres."

            is FirebaseAuthInvalidCredentialsException ->
                "E-mail ou senha inválidos."

            is FirebaseAuthUserCollisionException ->
                "Este e-mail já está cadastrado. Tente fazer login."

            is FirebaseAuthInvalidUserException ->
                "Usuário não encontrado ou conta desativada."

            is FirebaseNetworkException ->
                "Sem conexão com a internet. Verifique sua rede e tente novamente."

            is FirebaseAuthRecentLoginRequiredException ->
                "Por segurança, confirme sua senha atual antes de fazer essa alteração."

            else ->
                "Ocorreu um erro inesperado: ${e.message}"
        }
    }
}