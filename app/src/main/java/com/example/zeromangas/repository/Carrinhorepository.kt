package com.example.zeromangas.repository

import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@Serializable
private data class SalvarItemCarrinhoParamsDto(
    @SerialName("p_cliente_id") val clienteId: String,
    @SerialName("p_produto_id") val produtoId: String,
    @SerialName("p_quantidade") val quantidade: Int
)

@Serializable
private data class ClienteIdParamsDto(
    @SerialName("p_cliente_id") val clienteId: String
)

@Serializable
private data class RemoverItemCarrinhoParamsDto(
    @SerialName("p_cliente_id") val clienteId: String,
    @SerialName("p_produto_id") val produtoId: String
)

/** Item de carrinho salvo no banco: só o essencial (produto + quantidade). */
@Serializable
data class ItemCarrinhoDto(
    @SerialName("produto_id") val produtoId: String,
    val quantidade: Int
)

private val jsonRpc = Json { encodeDefaults = true }

/**
 * Persiste o carrinho na tabela "carrinho_itens", pra não se perder se o
 * usuário fechar o app. O [CartViewModel] continua sendo a fonte da verdade
 * em memória (pra UI reagir na hora) — este repository só espelha as
 * mudanças no banco em segundo plano, e recarrega no login.
 */
class CarrinhoRepository {

    suspend fun salvarItem(clienteId: String, produtoId: String, quantidade: Int): Result<Unit> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                SalvarItemCarrinhoParamsDto.serializer(),
                SalvarItemCarrinhoParamsDto(clienteId = clienteId, produtoId = produtoId, quantidade = quantidade)
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("salvar_item_carrinho", paramsJson)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listarItens(clienteId: String): Result<List<ItemCarrinhoDto>> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                ClienteIdParamsDto.serializer(),
                ClienteIdParamsDto(clienteId = clienteId)
            ).jsonObject

            val itens = SupabaseClient.client.postgrest
                .rpc("listar_carrinho", paramsJson)
                .decodeList<ItemCarrinhoDto>()

            Result.success(itens)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removerItem(clienteId: String, produtoId: String): Result<Unit> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                RemoverItemCarrinhoParamsDto.serializer(),
                RemoverItemCarrinhoParamsDto(clienteId = clienteId, produtoId = produtoId)
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("remover_item_carrinho", paramsJson)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun limparCarrinho(clienteId: String): Result<Unit> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                ClienteIdParamsDto.serializer(),
                ClienteIdParamsDto(clienteId = clienteId)
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("limpar_carrinho_db", paramsJson)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}