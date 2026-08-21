package com.example.zeromangas.repository

import com.example.zeromangas.data.model.CartItem
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.data.model.Order
import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Serializable
private data class PedidoDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("cliente_id") val clienteId: String? = null,
    @SerialName("endereco_id") val enderecoId: String? = null,
    @SerialName("valor_produtos") val valorProdutos: Double,
    @SerialName("valor_frete") val valorFrete: Double,
    @SerialName("valor_desconto") val valorDesconto: Double = 0.0,
    @SerialName("valor_total") val valorTotal: Double,
    @SerialName("tipo_frete") val tipoFrete: String,
    val cep: String,
    @SerialName("cupom_codigo") val cupomCodigo: String? = null,
    val data: String? = null,
    val status: String
)

@Serializable
private data class PedidoItemDto(
    @SerialName("produto_id") val produtoId: String,
    val quantidade: Int,
    @SerialName("preco_unitario") val precoUnitario: Double,
    @SerialName("produto_nome") val produtoNome: String? = null,
    @SerialName("produto_imagem_url") val produtoImagemUrl: String? = null
)

@Serializable
private data class ListarPedidosParamsDto(
    @SerialName("p_user_id") val userId: String
)

@Serializable
private data class ListarItensPedidoParamsDto(
    @SerialName("p_pedido_id") val pedidoId: String
)

// ---- DTOs para a RPC criar_pedido ----

@Serializable
private data class CriarPedidoItemDto(
    @SerialName("produto_id") val produtoId: String,
    val quantidade: Int,
    @SerialName("preco_unitario") val precoUnitario: Double
)

@Serializable
private data class CriarPedidoParamsDto(
    @SerialName("p_user_id") val userId: String,
    @SerialName("p_valor_produtos") val valorProdutos: Double,
    @SerialName("p_valor_frete") val valorFrete: Double,
    @SerialName("p_valor_desconto") val valorDesconto: Double,
    @SerialName("p_valor_total") val valorTotal: Double,
    @SerialName("p_tipo_frete") val tipoFrete: String,
    @SerialName("p_cep") val cep: String,
    @SerialName("p_cupom_codigo") val cupomCodigo: String = "",
    @SerialName("p_itens") val itens: List<CriarPedidoItemDto>,
    @SerialName("p_cliente_id") val clienteId: String? = null,
    @SerialName("p_endereco_id") val enderecoId: String? = null
)

// ---- DTO para a RPC cancelar_pedido ----

@Serializable
private data class CancelarPedidoParamsDto(
    @SerialName("p_pedido_id") val pedidoId: String
)

// ---- DTO para a RPC registrar_pagamento ----

@Serializable
private data class RegistrarPagamentoParamsDto(
    @SerialName("p_pedido_id") val pedidoId: String,
    @SerialName("p_metodo") val metodo: String,
    @SerialName("p_valor") val valor: Double
)

// Instância de Json configurada para SEMPRE incluir todos os campos ao serializar,
// mesmo os que têm valor igual ao padrão (ex: cupomCodigo = ""). Por padrão o
// kotlinx.serialization omite campos "no padrão", o que fazia a RPC do Postgres
// não encontrar a função por faltar o parâmetro p_cupom_codigo na chamada.
private val jsonRpc = Json { encodeDefaults = true }

class OrderRepository {

    /**
     * Cria o pedido chamando a RPC "criar_pedido" no Supabase.
     * A função no banco insere o pedido, os itens e desconta o estoque
     * dentro de UMA transação só: se qualquer etapa falhar (ex: estoque
     * insuficiente, item inválido), tudo é revertido automaticamente e
     * nenhum pedido "fantasma" fica salvo.
     */
    suspend fun salvarPedido(order: Order): Result<String> {
        return try {
            if (order.itens.isEmpty()) {
                return Result.failure(Exception("O pedido não possui itens."))
            }

            val params = CriarPedidoParamsDto(
                userId = order.userId,
                valorProdutos = order.valorProdutos,
                valorFrete = order.valorFrete,
                valorDesconto = order.valorDesconto,
                valorTotal = order.valorTotal,
                tipoFrete = order.tipoFrete,
                cep = order.cep,
                cupomCodigo = order.cupomCodigo, // pode ser "" — a coluna cupom_codigo é NOT NULL no banco
                itens = order.itens.map { item ->
                    CriarPedidoItemDto(
                        produtoId = item.manga.id,
                        quantidade = item.quantidade,
                        precoUnitario = item.manga.preco
                    )
                },
                clienteId = order.clienteId,
                enderecoId = order.enderecoId
            )

            val paramsJson = jsonRpc.encodeToJsonElement(
                CriarPedidoParamsDto.serializer(),
                params
            ).jsonObject

            val resultado = SupabaseClient.client.postgrest.rpc("criar_pedido", paramsJson)

            // A RPC retorna o uuid do pedido criado como uma string JSON simples
            // (ex: "\"3f2a...\""), então removemos as aspas antes de usar o valor.
            val pedidoId = jsonRpc.parseToJsonElement(resultado.data).jsonPrimitive.content

            Result.success(pedidoId)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível finalizar a compra.", e))
        }
    }

    suspend fun listarPedidosDoUsuario(userId: String): Result<List<Order>> {
        return try {
            val paramsPedidosJson = jsonRpc.encodeToJsonElement(
                ListarPedidosParamsDto.serializer(),
                ListarPedidosParamsDto(userId = userId)
            ).jsonObject

            val pedidosDto = SupabaseClient.client.postgrest
                .rpc("listar_pedidos_usuario", paramsPedidosJson)
                .decodeList<PedidoDto>()

            val pedidos = pedidosDto.map { pedidoDto ->
                val pedidoId = pedidoDto.id ?: ""

                val paramsItensJson = jsonRpc.encodeToJsonElement(
                    ListarItensPedidoParamsDto.serializer(),
                    ListarItensPedidoParamsDto(pedidoId = pedidoId)
                ).jsonObject

                val itensDto = SupabaseClient.client.postgrest
                    .rpc("listar_itens_pedido", paramsItensJson)
                    .decodeList<PedidoItemDto>()

                val itensCarrinho = itensDto.map { itemDto ->
                    CartItem(
                        manga = Manga(
                            id = itemDto.produtoId,
                            nome = itemDto.produtoNome ?: "Produto",
                            imagemUrl = itemDto.produtoImagemUrl ?: "",
                            preco = itemDto.precoUnitario
                        ),
                        quantidade = itemDto.quantidade
                    )
                }

                Order(
                    id = pedidoId,
                    userId = pedidoDto.userId,
                    clienteId = pedidoDto.clienteId,
                    enderecoId = pedidoDto.enderecoId,
                    itens = itensCarrinho,
                    valorProdutos = pedidoDto.valorProdutos,
                    valorFrete = pedidoDto.valorFrete,
                    valorDesconto = pedidoDto.valorDesconto,
                    valorTotal = pedidoDto.valorTotal,
                    tipoFrete = pedidoDto.tipoFrete,
                    cep = pedidoDto.cep,
                    cupomCodigo = pedidoDto.cupomCodigo ?: "",
                    data = isoParaMillis(pedidoDto.data ?: ""),
                    status = pedidoDto.status
                )
            }

            Result.success(pedidos)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível carregar os pedidos.", e))
        }
    }

    /**
     * Cancela um pedido chamando a RPC "cancelar_pedido" no Supabase.
     * A função no banco muda o status para "CANCELADO" E devolve o
     * estoque de cada item do pedido, numa transação só.
     * A tela deve permitir isso apenas enquanto o pedido ainda está "PROCESSANDO".
     */
    suspend fun cancelarPedido(pedidoId: String): Result<Unit> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                CancelarPedidoParamsDto.serializer(),
                CancelarPedidoParamsDto(pedidoId = pedidoId)
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("cancelar_pedido", paramsJson)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível cancelar o pedido.", e))
        }
    }

    /**
     * Registra o pagamento de um pedido já criado, chamando a RPC "registrar_pagamento".
     * É uma chamada separada de "criar_pedido" de propósito: se ela falhar por qualquer
     * motivo, o pedido em si já foi criado com sucesso e não deve ser desfeito por causa
     * disso — só logamos o erro e seguimos.
     */
    suspend fun registrarPagamento(pedidoId: String, metodo: String, valor: Double): Result<Unit> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                RegistrarPagamentoParamsDto.serializer(),
                RegistrarPagamentoParamsDto(pedidoId = pedidoId, metodo = metodo, valor = valor)
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("registrar_pagamento", paramsJson)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível registrar o pagamento.", e))
        }
    }

    private fun millisParaIso(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }

    private fun isoParaMillis(iso: String): Long {
        if (iso.isBlank()) return System.currentTimeMillis()
        return try {
            val semOffset = iso.replace(Regex("([+-]\\d{2}:?\\d{2}|Z)$"), "")
            val comMilissegundosAjustados = semOffset.replace(Regex("\\.(\\d{3})\\d*$"), ".$1")
            val padrao = if (comMilissegundosAjustados.contains(".")) {
                "yyyy-MM-dd'T'HH:mm:ss.SSS"
            } else {
                "yyyy-MM-dd'T'HH:mm:ss"
            }
            val sdf = SimpleDateFormat(padrao, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(comMilissegundosAjustados)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}