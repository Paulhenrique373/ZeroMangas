package com.example.zeromangas.repository

import com.example.zeromangas.data.model.CartItem
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.data.model.Order
import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Serializable
private data class ProdutoResumoDto(
    val id: String? = null,
    val nome: String = "",
    @SerialName("imagem_url") val imagemUrl: String = ""
)

@Serializable
private data class PedidoDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
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
private data class PedidoItemInsertDto(
    @SerialName("pedido_id") val pedidoId: String,
    @SerialName("produto_id") val produtoId: String,
    val quantidade: Int,
    @SerialName("preco_unitario") val precoUnitario: Double
)

@Serializable
private data class PedidoItemDto(
    @SerialName("produto_id") val produtoId: String,
    val quantidade: Int,
    @SerialName("preco_unitario") val precoUnitario: Double,
    val produtos: ProdutoResumoDto? = null
)

@Serializable
private data class StatusUpdateDto(val status: String)

class OrderRepository {

    private val pedidosTable = SupabaseClient.client.postgrest.from("pedidos")
    private val itensTable = SupabaseClient.client.postgrest.from("pedido_itens")

    /**
     * Salva o pedido na tabela "pedidos" e, em seguida, cada item do carrinho
     * como uma linha separada em "pedido_itens" ligada ao pedido criado.
     */
    suspend fun salvarPedido(order: Order): Result<String> {
        return try {
            val pedidoDto = PedidoDto(
                userId = order.userId,
                valorProdutos = order.valorProdutos,
                valorFrete = order.valorFrete,
                valorDesconto = order.valorDesconto,
                valorTotal = order.valorTotal,
                tipoFrete = order.tipoFrete,
                cep = order.cep,
                cupomCodigo = order.cupomCodigo.ifBlank { null },
                data = millisParaIso(order.data),
                status = order.status
            )

            val pedidoCriado = pedidosTable.insert(pedidoDto) {
                select()
            }.decodeSingle<PedidoDto>()

            val pedidoId = pedidoCriado.id
                ?: return Result.failure(Exception("Não foi possível obter o id do pedido criado."))

            if (order.itens.isNotEmpty()) {
                val itensDto = order.itens.map { item ->
                    PedidoItemInsertDto(
                        pedidoId = pedidoId,
                        produtoId = item.manga.id,
                        quantidade = item.quantidade,
                        precoUnitario = item.manga.preco
                    )
                }
                itensTable.insert(itensDto)
            }

            Result.success(pedidoId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listarPedidosDoUsuario(userId: String): Result<List<Order>> {
        return try {
            val pedidosDto = pedidosTable
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<PedidoDto>()

            val pedidos = pedidosDto.map { pedidoDto ->
                val pedidoId = pedidoDto.id ?: ""

                val itensDto = itensTable
                    .select(columns = Columns.raw("produto_id, quantidade, preco_unitario, produtos(id, nome, imagem_url)")) {
                        filter { eq("pedido_id", pedidoId) }
                    }
                    .decodeList<PedidoItemDto>()

                val itensCarrinho = itensDto.map { itemDto ->
                    CartItem(
                        manga = Manga(
                            id = itemDto.produtoId,
                            nome = itemDto.produtos?.nome ?: "Produto",
                            imagemUrl = itemDto.produtos?.imagemUrl ?: "",
                            preco = itemDto.precoUnitario
                        ),
                        quantidade = itemDto.quantidade
                    )
                }

                Order(
                    id = pedidoId,
                    userId = pedidoDto.userId,
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
            Result.failure(e)
        }
    }

    /**
     * Cancela um pedido, alterando o campo "status" para "CANCELADO".
     * A tela deve permitir isso apenas enquanto o pedido ainda está "Processando".
     */
    suspend fun cancelarPedido(pedidoId: String): Result<Unit> {
        return try {
            pedidosTable.update(StatusUpdateDto(status = "CANCELADO")) {
                filter { eq("id", pedidoId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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