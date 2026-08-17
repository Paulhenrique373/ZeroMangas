package com.example.zeromangas.repository

import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FavoritoDto(
    val id: String? = null,
    @SerialName("usuario_id") val usuarioId: String,
    @SerialName("produto_id") val produtoId: String
)

/**
 * Guarda os favoritos de cada usuário na tabela "favoritos" do Supabase (Postgres).
 * Como o id da linha é um UUID aleatório (não composto como era no Firestore),
 * verificamos se o favorito já existe antes de inserir, evitando duplicados.
 */
class FavoritoRepository {

    private val favoritosTable = SupabaseClient.client.postgrest.from("favoritos")

    suspend fun adicionarFavorito(usuarioId: String, produtoId: String): Result<Unit> {
        return try {
            val existentes = favoritosTable
                .select {
                    filter {
                        eq("usuario_id", usuarioId)
                        eq("produto_id", produtoId)
                    }
                }
                .decodeList<FavoritoDto>()

            if (existentes.isEmpty()) {
                favoritosTable.insert(
                    FavoritoDto(usuarioId = usuarioId, produtoId = produtoId)
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removerFavorito(usuarioId: String, produtoId: String): Result<Unit> {
        return try {
            favoritosTable.delete {
                filter {
                    eq("usuario_id", usuarioId)
                    eq("produto_id", produtoId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retorna a lista de ids de produtos favoritados pelo usuário.
     */
    suspend fun listarFavoritos(usuarioId: String): Result<List<String>> {
        return try {
            val lista = favoritosTable
                .select {
                    filter {
                        eq("usuario_id", usuarioId)
                    }
                }
                .decodeList<FavoritoDto>()
            Result.success(lista.map { it.produtoId })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}