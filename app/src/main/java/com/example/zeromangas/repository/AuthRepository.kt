package com.example.zeromangas.repository

import android.net.Uri
import com.example.zeromangas.data.model.User
import com.google.firebase.auth.FirebaseAuth
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
            Result.failure(e)
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
            Result.failure(e)
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
            Result.failure(e)
        }
    }
}