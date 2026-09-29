package com.example.zeromangas.repository

import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/** Linha da lista de pedidos do admin (RPC "admin_listar_pedidos"). */
@Serializable
data class PedidoAdminDto(
    @SerialName("id_pedido") val idPedido: String,
    @SerialName("nome_cliente") val nomeCliente: String? = null,
    @SerialName("email_cliente") val emailCliente: String? = null,
    @SerialName("valor_produtos") val valorProdutos: Double = 0.0,
    @SerialName("valor_frete") val valorFrete: Double = 0.0,
    @SerialName("valor_desconto") val valorDesconto: Double = 0.0,
    @SerialName("valor_total") val valorTotal: Double = 0.0,
    @SerialName("tipo_frete") val tipoFrete: String? = null,
    val cep: String? = null,
    @SerialName("cupom_codigo") val cupomCodigo: String? = null,
    @SerialName("data_pedido") val dataPedido: String? = null,
    val status: String,
    @SerialName("qtd_itens") val qtdItens: Long = 0,
    @SerialName("metodo_pagamento") val metodoPagamento: String? = null,
    @SerialName("status_pagamento") val statusPagamento: String? = null,
    @SerialName("endereco_entrega") val enderecoEntrega: String? = null
)

/** Item de um pedido (RPC "admin_listar_itens_pedido"). */
@Serializable
data class ItemPedidoAdminDto(
    @SerialName("produto_id") val produtoId: String,
    val quantidade: Int,
    @SerialName("preco_unitario") val precoUnitario: Double,
    val subtotal: Double = 0.0,
    @SerialName("produto_nome") val produtoNome: String? = null,
    @SerialName("produto_imagem_url") val produtoImagemUrl: String? = null
)

@Serializable
private data class ListarPedidosAdminParamsDto(
    @SerialName("p_status") val status: String
)

@Serializable
private data class PedidoIdParamsDto(
    @SerialName("p_id_pedido") val idPedido: String
)

@Serializable
private data class MudarStatusParamsDto(
    @SerialName("p_id_pedido") val idPedido: String,
    @SerialName("p_status") val status: String
)

private val jsonRpc = Json { encodeDefaults = true }

/**
 * Pedidos no painel administrativo (item 5). Todas as RPCs reconfirmam sou_admin()
 * no banco — o gate da tela é só conveniência de interface.
 */
class AdminPedidosRepository {

    /** [status] vazio = todos os pedidos. */
    suspend fun listarPedidos(status: String): Result<List<PedidoAdminDto>> {
        return try {
            val params = jsonRpc.encodeToJsonElement(
                ListarPedidosAdminParamsDto.serializer(),
                ListarPedidosAdminParamsDto(status = status)
            ).jsonObject

            val lista = SupabaseClient.client.postgrest
                .rpc("admin_listar_pedidos", params)
                .decodeList<PedidoAdminDto>()

            Result.success(lista)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível carregar os pedidos.", e))
        }
    }

    suspend fun listarItens(idPedido: String): Result<List<ItemPedidoAdminDto>> {
        return try {
            val params = jsonRpc.encodeToJsonElement(
                PedidoIdParamsDto.serializer(),
                PedidoIdParamsDto(idPedido = idPedido)
            ).jsonObject

            val itens = SupabaseClient.client.postgrest
                .rpc("admin_listar_itens_pedido", params)
                .decodeList<ItemPedidoAdminDto>()

            Result.success(itens)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível carregar os itens do pedido.", e))
        }
    }

    suspend fun mudarStatus(idPedido: String, novoStatus: String): Result<Unit> {
        return try {
            val params = jsonRpc.encodeToJsonElement(
                MudarStatusParamsDto.serializer(),
                MudarStatusParamsDto(idPedido = idPedido, status = novoStatus)
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("admin_mudar_status_pedido", params)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível mudar o status do pedido.", e))
        }
    }

    suspend fun cancelarPedido(idPedido: String): Result<Unit> {
        return try {
            val params = jsonRpc.encodeToJsonElement(
                PedidoIdParamsDto.serializer(),
                PedidoIdParamsDto(idPedido = idPedido)
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("admin_cancelar_pedido", params)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível cancelar o pedido.", e))
        }
    }
}