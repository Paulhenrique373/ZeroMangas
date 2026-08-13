package com.example.zeromangas.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Guarda os favoritos de cada usuário na coleção "favoritos".
 * O id do documento é "usuarioId_mangaId", o que evita duplicados
 * e torna a remoção uma operação direta por id (sem precisar de query).
 */
class FavoritoRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val favoritosCollection = db.collection("favoritos")

    private fun idDocumento(usuarioId: String, mangaId: String) = "${usuarioId}_$mangaId"

    suspend fun adicionarFavorito(usuarioId: String, mangaId: String): Result<Unit> {
        return try {
            val dados = hashMapOf(
                "usuarioId" to usuarioId,
                "mangaId" to mangaId,
                "data" to System.currentTimeMillis()
            )
            favoritosCollection.document(idDocumento(usuarioId, mangaId)).set(dados).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removerFavorito(usuarioId: String, mangaId: String): Result<Unit> {
        return try {
            favoritosCollection.document(idDocumento(usuarioId, mangaId)).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retorna a lista de ids de mangás favoritados pelo usuário.
     */
    suspend fun listarFavoritos(usuarioId: String): Result<List<String>> {
        return try {
            val snapshot = favoritosCollection
                .whereEqualTo("usuarioId", usuarioId)
                .get()
                .await()
            val ids = snapshot.documents.mapNotNull { it.getString("mangaId") }
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}