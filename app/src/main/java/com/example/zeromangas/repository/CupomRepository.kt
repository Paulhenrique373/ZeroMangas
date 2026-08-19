package com.example.zeromangas.repository

import com.example.zeromangas.data.model.Cupom
import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class CupomDto(
    val id: String? = null,
    val codigo: String,
    @SerialName("tipo_desconto") val tipoDesconto: String = "PERCENTUAL",
    val valor: Double = 0.0,
    val ativo: Boolean = true,
    @SerialName("valor_minimo") val valorMinimo: Double = 0.0,
    @SerialName("limite_total") val limiteTotal: Int = 0
) {
    fun paraCupom() = Cupom(
        codigo = codigo,
        tipoDesconto = tipoDesconto,
        valor = valor,
        ativo = ativo,
        valorMinimo = valorMinimo,
        limiteTotal = limiteTotal
    )
}

@Serializable
private data class PedidoUsoDto(
    val id: String? = null
)

class CupomRepository {

    private val cuponsTable = SupabaseClient.client.postgrest.from("cupons")
    private val pedidosTable = SupabaseClient.client.postgrest.from("pedidos")

    /**
     * Busca um cupom pelo código (sem diferenciar maiúsculas/minúsculas) na tabela "cupons".
     * Retorna erro se o cupom não existir ou estiver inativo.
     */
    suspend fun buscarCupom(codigo: String): Result<Cupom> {
        return try {
            val codigoNormalizado = codigo.trim()

            val lista = cuponsTable
                .select {
                    filter {
                        ilike("codigo", codigoNormalizado)
                    }
                }
                .decodeList<CupomDto>()

            val cupomDto = lista.firstOrNull()

            when {
                cupomDto == null -> Result.failure(Exception("Cupom inválido."))
                !cupomDto.ativo -> Result.failure(Exception("Este cupom não está mais ativo."))
                else -> Result.success(cupomDto.paraCupom())
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível validar o cupom. Tente novamente.", e))
        }
    }

    /**
     * Conta quantas vezes um cupom já foi utilizado no total, em todos os pedidos,
     * consultando a coluna "cupom_codigo" da tabela "pedidos".
     * Pedidos CANCELADOS não contam como uso, já que o cancelamento devolve
     * estoque e desfaz o pedido — o cupom deve ficar disponível de novo.
     */
    suspend fun contarUsosTotais(codigo: String): Result<Int> {
        return try {
            val lista = pedidosTable
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("cupom_codigo", codigo)
                        neq("status", "CANCELADO")
                    }
                }
                .decodeList<PedidoUsoDto>()

            Result.success(lista.size)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível verificar o uso total do cupom.", e))
        }
    }

    /**
     * Conta quantas vezes um usuário específico já utilizou este cupom,
     * consultando "cupom_codigo" e "user_id" na tabela "pedidos".
     * Pedidos CANCELADOS não contam como uso (mesmo motivo do método acima).
     */
    suspend fun contarUsosPorUsuario(codigo: String, userId: String): Result<Int> {
        return try {
            val lista = pedidosTable
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("cupom_codigo", codigo)
                        eq("user_id", userId)
                        neq("status", "CANCELADO")
                    }
                }
                .decodeList<PedidoUsoDto>()

            Result.success(lista.size)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível verificar o uso do cupom para este usuário.", e))
        }
    }
}