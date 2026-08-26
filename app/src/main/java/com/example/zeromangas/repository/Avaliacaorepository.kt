package com.example.zeromangas.repository

import com.example.zeromangas.data.model.Avaliacao
import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@Serializable
private data class AvaliarProdutoParamsDto(
    @SerialName("p_produto_id") val produtoId: String,
    @SerialName("p_cliente_id") val clienteId: String,
    @SerialName("p_nota") val nota: Int,
    @SerialName("p_comentario") val comentario: String?
)

@Serializable
private data class ListarAvaliacoesParamsDto(
    @SerialName("p_produto_id") val produtoId: String
)

@Serializable
private data class ExcluirAvaliacaoParamsDto(
    @SerialName("p_avaliacao_id") val avaliacaoId: String,
    @SerialName("p_cliente_id") val clienteId: String
)

@Serializable
private data class AvaliacaoDto(
    val id: String,
    val nota: Int,
    val comentario: String? = null,
    @SerialName("criado_em") val criadoEm: String,
    @SerialName("nome_cliente") val nomeCliente: String? = null
)

private val jsonRpc = Json { encodeDefaults = true }
private val jsonRpcOmitindoNulos = Json { encodeDefaults = false; explicitNulls = false }

private fun AvaliacaoDto.paraModel(): Avaliacao = Avaliacao(
    id = id,
    nota = nota,
    comentario = comentario ?: "",
    criadoEm = criadoEm,
    nomeCliente = nomeCliente ?: "Cliente"
)

/**
 * Avaliações (nota 1-5 + comentário) por produto. Uma avaliação por cliente
 * por produto — enviar de novo atualiza a existente (upsert na RPC). A média
 * do produto é recalculada automaticamente por um trigger no banco, então
 * este repository não precisa (nem deve) atualizar "nota_media" manualmente.
 */
class AvaliacaoRepository {

    suspend fun avaliarProduto(
        produtoId: String,
        clienteId: String,
        nota: Int,
        comentario: String?
    ): Result<Unit> {
        return try {
            val paramsJson = jsonRpcOmitindoNulos.encodeToJsonElement(
                AvaliarProdutoParamsDto.serializer(),
                AvaliarProdutoParamsDto(
                    produtoId = produtoId,
                    clienteId = clienteId,
                    nota = nota.coerceIn(1, 5),
                    comentario = comentario?.ifBlank { null }
                )
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("avaliar_produto", paramsJson)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Não foi possível enviar sua avaliação.", e))
        }
    }

    suspend fun listarAvaliacoes(produtoId: String): Result<List<Avaliacao>> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                ListarAvaliacoesParamsDto.serializer(),
                ListarAvaliacoesParamsDto(produtoId = produtoId)
            ).jsonObject

            val avaliacoes = SupabaseClient.client.postgrest
                .rpc("listar_avaliacoes", paramsJson)
                .decodeList<AvaliacaoDto>()
                .map { it.paraModel() }

            Result.success(avaliacoes)
        } catch (e: Exception) {
            Result.failure(Exception("Não foi possível carregar as avaliações.", e))
        }
    }

    suspend fun excluirAvaliacao(avaliacaoId: String, clienteId: String): Result<Unit> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                ExcluirAvaliacaoParamsDto.serializer(),
                ExcluirAvaliacaoParamsDto(avaliacaoId = avaliacaoId, clienteId = clienteId)
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("excluir_avaliacao", paramsJson)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Não foi possível excluir a avaliação.", e))
        }
    }
}