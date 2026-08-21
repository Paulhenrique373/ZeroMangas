package com.example.zeromangas.repository

import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@Serializable
private data class UsuarioProdutoParamsDto(
    @SerialName("p_usuario_id") val usuarioId: String,
    @SerialName("p_produto_id") val produtoId: String
)

@Serializable
private data class ListarFavoritosParamsDto(
    @SerialName("p_usuario_id") val usuarioId: String
)

@Serializable
private data class FavoritoProdutoIdDto(
    @SerialName("produto_id") val produtoId: String
)

private val jsonRpc = Json { encodeDefaults = true }

/**
 * Guarda os favoritos de cada usuário na tabela "favoritos" do Supabase (Postgres).
 * A tabela tem RLS habilitada sem policy — toda leitura/escrita passa pelas funções
 * "adicionar_favorito", "remover_favorito" e "listar_favoritos" (security definer).
 */
class FavoritoRepository {

    suspend fun adicionarFavorito(usuarioId: String, produtoId: String): Result<Unit> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                UsuarioProdutoParamsDto.serializer(),
                UsuarioProdutoParamsDto(usuarioId = usuarioId, produtoId = produtoId)
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("adicionar_favorito", paramsJson)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removerFavorito(usuarioId: String, produtoId: String): Result<Unit> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                UsuarioProdutoParamsDto.serializer(),
                UsuarioProdutoParamsDto(usuarioId = usuarioId, produtoId = produtoId)
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("remover_favorito", paramsJson)
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
            val paramsJson = jsonRpc.encodeToJsonElement(
                ListarFavoritosParamsDto.serializer(),
                ListarFavoritosParamsDto(usuarioId = usuarioId)
            ).jsonObject

            val lista = SupabaseClient.client.postgrest
                .rpc("listar_favoritos", paramsJson)
                .decodeList<FavoritoProdutoIdDto>()

            Result.success(lista.map { it.produtoId })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}