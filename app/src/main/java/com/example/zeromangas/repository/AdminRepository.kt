package com.example.zeromangas.repository

import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * Resumo de números da loja pro Dashboard administrativo, retornado pela RPC
 * "admin_dashboard_resumo". "produtos_estoque_baixo"/"produtos_esgotados" são
 * uma foto do estoque atual (não mudam com o filtro de período); os demais
 * campos são calculados só sobre o período selecionado.
 */
@Serializable
data class DashboardResumoDto(
    @SerialName("faturamento_total") val faturamentoTotal: Double = 0.0,
    @SerialName("total_pedidos") val totalPedidos: Long = 0,
    @SerialName("ticket_medio") val ticketMedio: Double = 0.0,
    @SerialName("pedidos_cancelados") val pedidosCancelados: Long = 0,
    @SerialName("novos_clientes") val novosClientes: Long = 0,
    @SerialName("produtos_estoque_baixo") val produtosEstoqueBaixo: Long = 0,
    @SerialName("produtos_esgotados") val produtosEsgotados: Long = 0
)

@Serializable
private data class DashboardResumoParamsDto(
    @SerialName("p_dias") val dias: Int
)

private val jsonRpc = Json { encodeDefaults = true }

/**
 * Verificações de autorização administrativa. A fonte da verdade é sempre o banco
 * (tabelas "perfis"/"usuario_perfil", checadas pela função sou_admin() no Supabase,
 * que olha o auth.uid() da sessão atual) — nunca um valor local guardado no app.
 * Qualquer tela/rota administrativa deve confirmar por aqui antes de mostrar dados
 * ou ações sensíveis; a proteção de verdade continua sendo o RLS do banco, isso
 * aqui só decide o que a interface mostra.
 */
class AdminRepository {

    suspend fun souAdmin(): Result<Boolean> {
        return try {
            val resultado = SupabaseClient.client.postgrest.rpc("sou_admin")
            Result.success(resultado.data.trim().toBooleanStrictOrNull() ?: false)
        } catch (e: Exception) {
            // Se der erro de rede/etc, trata como "não é admin" — nunca libera acesso
            // administrativo por causa de uma falha ao verificar.
            Result.failure(e)
        }
    }

    /**
     * Busca os números do Dashboard (faturamento, pedidos, clientes novos, estoque
     * baixo/esgotado...) pro período de [dias] dias. A RPC no banco também reconfirma
     * sou_admin() antes de calcular qualquer coisa — não depende só do gate da tela.
     */
    suspend fun buscarResumoDashboard(dias: Int): Result<DashboardResumoDto> {
        return try {
            val params = jsonRpc.encodeToJsonElement(
                DashboardResumoParamsDto.serializer(),
                DashboardResumoParamsDto(dias = dias)
            ).jsonObject

            val resultado = SupabaseClient.client.postgrest
                .rpc("admin_dashboard_resumo", params)
                .decodeSingle<DashboardResumoDto>()

            Result.success(resultado)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível carregar o dashboard.", e))
        }
    }
}