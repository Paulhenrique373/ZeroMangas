package com.example.zeromangas.repository

import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * Cliente na visão do admin (RPC "admin_listar_clientes"). Só dados básicos: a RPC
 * nunca devolve senha, CPF nem telefone.
 */
@Serializable
data class ClienteAdminDto(
    @SerialName("id_usuario") val idUsuario: String,
    val nome: String? = null,
    val email: String? = null,
    @SerialName("data_cadastro") val dataCadastro: String? = null,
    val status: String? = null,
    @SerialName("qtd_pedidos") val qtdPedidos: Long = 0,
    @SerialName("ultimo_pedido") val ultimoPedido: String? = null
)

@Serializable
private data class ListarClientesParamsDto(
    @SerialName("p_busca") val busca: String
)

private val jsonRpc = Json { encodeDefaults = true }

class AdminClientesRepository {

    /** [busca] vazio = todos; senão filtra por nome ou e-mail. */
    suspend fun listarClientes(busca: String): Result<List<ClienteAdminDto>> {
        return try {
            val params = jsonRpc.encodeToJsonElement(
                ListarClientesParamsDto.serializer(),
                ListarClientesParamsDto(busca = busca.trim())
            ).jsonObject

            val lista = SupabaseClient.client.postgrest
                .rpc("admin_listar_clientes", params)
                .decodeList<ClienteAdminDto>()

            Result.success(lista)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível carregar os clientes.", e))
        }
    }
}