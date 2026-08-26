package com.example.zeromangas.repository

import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@Serializable
private data class RegistrarPesquisaParamsDto(
    @SerialName("p_usuario_id") val usuarioId: String,
    @SerialName("p_termo") val termo: String
)

@Serializable
private data class PesquisaDto(
    val termo: String,
    @SerialName("criado_em") val criadoEm: String
)

private val jsonRpc = Json { encodeDefaults = true }

/**
 * Histórico de pesquisas do usuário (tabela "pesquisas_recentes"). O banco já
 * mantém só as 15 mais recentes por usuário (ver função "registrar_pesquisa"),
 * então aqui é só registrar/listar/limpar.
 */
class PesquisaRepository {

    suspend fun registrarPesquisa(usuarioId: String, termo: String): Result<Unit> {
        if (termo.isBlank()) return Result.success(Unit)
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                RegistrarPesquisaParamsDto.serializer(),
                RegistrarPesquisaParamsDto(usuarioId = usuarioId, termo = termo.trim())
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("registrar_pesquisa", paramsJson) { }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listarPesquisasRecentes(usuarioId: String): Result<List<String>> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                UsuarioParamsDto.serializer(),
                UsuarioParamsDto(usuarioId = usuarioId)
            ).jsonObject

            val termos = SupabaseClient.client.postgrest
                .rpc("listar_pesquisas_recentes", paramsJson) { }
                .decodeList<PesquisaDto>()
                .map { it.termo }

            Result.success(termos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun limparHistorico(usuarioId: String): Result<Unit> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                UsuarioParamsDto.serializer(),
                UsuarioParamsDto(usuarioId = usuarioId)
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("limpar_pesquisas_recentes", paramsJson) { }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}