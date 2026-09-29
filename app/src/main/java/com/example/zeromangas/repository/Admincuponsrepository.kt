package com.example.zeromangas.repository

import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/** Cupom na visão do admin (RPC "admin_listar_cupons"), com contagem de usos. */
@Serializable
data class CupomAdminDto(
    val id: String,
    val codigo: String,
    @SerialName("tipo_desconto") val tipoDesconto: String = "PERCENTUAL",
    val valor: Double = 0.0,
    val ativo: Boolean = true,
    @SerialName("valor_minimo") val valorMinimo: Double = 0.0,
    @SerialName("limite_total") val limiteTotal: Int = 0,
    val usos: Long = 0
)

@Serializable
private data class CriarCupomParamsDto(
    @SerialName("p_codigo") val codigo: String,
    @SerialName("p_tipo_desconto") val tipoDesconto: String,
    @SerialName("p_valor") val valor: Double,
    @SerialName("p_valor_minimo") val valorMinimo: Double,
    @SerialName("p_limite_total") val limiteTotal: Int
)

@Serializable
private data class AtualizarCupomParamsDto(
    @SerialName("p_id") val id: String,
    @SerialName("p_tipo_desconto") val tipoDesconto: String,
    @SerialName("p_valor") val valor: Double,
    @SerialName("p_valor_minimo") val valorMinimo: Double,
    @SerialName("p_limite_total") val limiteTotal: Int
)

@Serializable
private data class AlternarCupomParamsDto(
    @SerialName("p_id") val id: String,
    @SerialName("p_ativo") val ativo: Boolean
)

private val jsonRpc = Json { encodeDefaults = true }

/**
 * Gestão de cupons no painel admin. Toda escrita passa por RPC (com sou_admin() no
 * banco) — nunca INSERT/UPDATE direto na tabela pelo cliente. O leitor de cupons do
 * checkout continua sendo o [CupomRepository].
 */
class AdminCuponsRepository {

    suspend fun listarCupons(): Result<List<CupomAdminDto>> {
        return try {
            val lista = SupabaseClient.client.postgrest
                .rpc("admin_listar_cupons")
                .decodeList<CupomAdminDto>()
            Result.success(lista)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível carregar os cupons.", e))
        }
    }

    suspend fun criarCupom(
        codigo: String,
        tipoDesconto: String,
        valor: Double,
        valorMinimo: Double,
        limiteTotal: Int
    ): Result<Unit> {
        return try {
            val params = jsonRpc.encodeToJsonElement(
                CriarCupomParamsDto.serializer(),
                CriarCupomParamsDto(codigo, tipoDesconto, valor, valorMinimo, limiteTotal)
            ).jsonObject
            SupabaseClient.client.postgrest.rpc("admin_criar_cupom", params)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível criar o cupom.", e))
        }
    }

    suspend fun atualizarCupom(
        id: String,
        tipoDesconto: String,
        valor: Double,
        valorMinimo: Double,
        limiteTotal: Int
    ): Result<Unit> {
        return try {
            val params = jsonRpc.encodeToJsonElement(
                AtualizarCupomParamsDto.serializer(),
                AtualizarCupomParamsDto(id, tipoDesconto, valor, valorMinimo, limiteTotal)
            ).jsonObject
            SupabaseClient.client.postgrest.rpc("admin_atualizar_cupom", params)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível atualizar o cupom.", e))
        }
    }

    suspend fun alternarAtivo(id: String, ativo: Boolean): Result<Unit> {
        return try {
            val params = jsonRpc.encodeToJsonElement(
                AlternarCupomParamsDto.serializer(),
                AlternarCupomParamsDto(id, ativo)
            ).jsonObject
            SupabaseClient.client.postgrest.rpc("admin_alternar_ativo_cupom", params)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível alterar o cupom.", e))
        }
    }
}