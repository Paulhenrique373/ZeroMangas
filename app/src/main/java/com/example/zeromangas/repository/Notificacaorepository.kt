package com.example.zeromangas.repository

import com.example.zeromangas.data.model.Notificacao
import com.example.zeromangas.data.model.paraTipoNotificacao
import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@Serializable
private data class NotificacaoIdParamsDto(
    @SerialName("p_notificacao_id") val notificacaoId: String
)

@Serializable
private data class NotificacaoDto(
    val id: String,
    @SerialName("usuario_id") val usuarioId: String? = null,
    val tipo: String,
    val titulo: String,
    val mensagem: String,
    @SerialName("produto_id") val produtoId: String? = null,
    @SerialName("pedido_id") val pedidoId: String? = null,
    val lida: Boolean = false,
    @SerialName("criado_em") val criadoEm: String
)

private val jsonRpc = Json { encodeDefaults = true }

private fun NotificacaoDto.paraModel(): Notificacao = Notificacao(
    id = id,
    tipo = tipo.paraTipoNotificacao(),
    titulo = titulo,
    mensagem = mensagem,
    produtoId = produtoId,
    pedidoId = pedidoId,
    lida = lida,
    criadoEm = criadoEm
)

/**
 * Notificações do usuário (promoção, lançamento, pedido, favorito voltou ao
 * estoque/entrou em promoção). A tabela "notificacoes" já vem alimentada por
 * triggers no banco (ver Etapa 1 do SQL) — este repository só lê e escuta em
 * tempo real via Supabase Realtime, além de marcar como lida.
 */
class NotificacaoRepository {

    private val realtimeChannel by lazy {
        SupabaseClient.client.realtime.channel("notificacoes-canal") { }
    }

    suspend fun listarNotificacoes(usuarioId: String): Result<List<Notificacao>> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                UsuarioParamsDto.serializer(),
                UsuarioParamsDto(usuarioId = usuarioId)
            ).jsonObject

            val notificacoes = SupabaseClient.client.postgrest
                .rpc("listar_notificacoes", paramsJson) { }
                .decodeList<NotificacaoDto>()
                .map { it.paraModel() }

            Result.success(notificacoes)
        } catch (e: Exception) {
            Result.failure(Exception("Não foi possível carregar as notificações.", e))
        }
    }

    suspend fun marcarComoLida(notificacaoId: String): Result<Unit> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                NotificacaoIdParamsDto.serializer(),
                NotificacaoIdParamsDto(notificacaoId = notificacaoId)
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("marcar_notificacao_lida", paramsJson) { }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun marcarTodasComoLidas(usuarioId: String): Result<Unit> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                UsuarioParamsDto.serializer(),
                UsuarioParamsDto(usuarioId = usuarioId)
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("marcar_todas_notificacoes_lidas", paramsJson) { }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Escuta em tempo real (Supabase Realtime) por novas linhas inseridas em
     * "notificacoes" que sejam deste usuário (pessoal) ou gerais (usuario_id
     * nulo — promoção/lançamento pra todo mundo). O filtro do canal já traz só
     * INSERTs; o filtro de dono é feito aqui porque o Realtime não suporta
     * "coluna = X OU coluna IS NULL" na assinatura do canal.
     */
    fun escutarNovasNotificacoes(usuarioId: String): Flow<Notificacao> {
        return realtimeChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "notificacoes"
        }
            .map { it.decodeRecord<NotificacaoDto>() }
            .filter { it.usuarioId == usuarioId || it.usuarioId == null }
            .map { it.paraModel() }
    }

    suspend fun conectarCanal() {
        if (realtimeChannel.status.value != io.github.jan.supabase.realtime.RealtimeChannel.Status.SUBSCRIBED) {
            realtimeChannel.subscribe()
        }
    }

    suspend fun desconectarCanal() {
        realtimeChannel.unsubscribe()
    }
}