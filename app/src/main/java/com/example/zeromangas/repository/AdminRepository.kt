package com.example.zeromangas.repository

import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

/**
 * Números "de agora" do Dashboard (RPC "admin_dashboard_extras"): não dependem do
 * período selecionado.
 */
@Serializable
data class DashboardExtrasDto(
    @SerialName("total_clientes") val totalClientes: Long = 0,
    @SerialName("total_produtos") val totalProdutos: Long = 0,
    @SerialName("pedidos_pendentes") val pedidosPendentes: Long = 0,
    @SerialName("pedidos_em_preparacao") val pedidosEmPreparacao: Long = 0
)

@Serializable
private data class DashboardResumoParamsDto(
    @SerialName("p_dias") val dias: Int
)

@Serializable
private data class CriarProdutoParamsDto(
    @SerialName("p_nome") val nome: String,
    @SerialName("p_marca_id") val marcaId: String,
    @SerialName("p_categoria_id") val categoriaId: String,
    @SerialName("p_volume") val volume: Int,
    @SerialName("p_preco") val preco: Double,
    @SerialName("p_imagem_url") val imagemUrl: String,
    @SerialName("p_descricao") val descricao: String,
    @SerialName("p_estoque") val estoque: Int,
    @SerialName("p_autor") val autor: String,
    @SerialName("p_em_destaque") val emDestaque: Boolean
)

@Serializable
private data class AtualizarProdutoParamsDto(
    @SerialName("p_id_produto") val idProduto: String,
    @SerialName("p_nome") val nome: String,
    @SerialName("p_marca_id") val marcaId: String,
    @SerialName("p_categoria_id") val categoriaId: String,
    @SerialName("p_volume") val volume: Int,
    @SerialName("p_preco") val preco: Double,
    @SerialName("p_imagem_url") val imagemUrl: String,
    @SerialName("p_descricao") val descricao: String,
    @SerialName("p_estoque") val estoque: Int,
    @SerialName("p_autor") val autor: String,
    @SerialName("p_em_destaque") val emDestaque: Boolean
)

@Serializable
private data class AlternarAtivoProdutoParamsDto(
    @SerialName("p_id_produto") val idProduto: String,
    @SerialName("p_ativo") val ativo: Boolean
)

@Serializable
private data class NomeParamsDto(
    @SerialName("p_nome") val nome: String
)

@Serializable
private data class AtualizarNomeParamsDto(
    @SerialName("p_id") val id: String,
    @SerialName("p_nome") val nome: String
)

@Serializable
private data class AjustarEstoqueParamsDto(
    @SerialName("p_id_produto") val idProduto: String,
    @SerialName("p_estoque") val estoque: Int
)

private val jsonRpc = Json { encodeDefaults = true }

/**
 * Verificações de autorização administrativa. A fonte da verdade é sempre o banco
 * (tabelas "perfis"/"usuario_perfil", checadas pela função sou_admin() no Supabase,
 * que olha o auth.uid() da sessão atual) — nunca um valor local guardado no app.
 * Qualquer tela/rota administrativa deve confirmar por aqui antes de mostrar dados
 * ou ações sensíveis; a proteção de verdade continua sendo o RLS do banco, isso
 * aqui só decide o que a interface mostra. Toda escrita administrativa passa por
 * RPC "security definer" que reconfirma sou_admin() no banco antes de qualquer
 * alteração — nunca por UPDATE/INSERT direto na tabela pelo cliente.
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

    /** Totais e pendências atuais da loja (RPC "admin_dashboard_extras"). */
    suspend fun buscarExtrasDashboard(): Result<DashboardExtrasDto> {
        return try {
            val resultado = SupabaseClient.client.postgrest
                .rpc("admin_dashboard_extras")
                .decodeSingle<DashboardExtrasDto>()
            Result.success(resultado)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível carregar os totais.", e))
        }
    }

    /** Cria um produto novo (RPC "admin_criar_produto") e retorna o id gerado. */
    suspend fun criarProduto(
        nome: String,
        marcaId: String,
        categoriaId: String,
        volume: Int,
        preco: Double,
        imagemUrl: String,
        descricao: String,
        estoque: Int,
        autor: String,
        emDestaque: Boolean
    ): Result<String> {
        return try {
            val params = jsonRpc.encodeToJsonElement(
                CriarProdutoParamsDto.serializer(),
                CriarProdutoParamsDto(
                    nome = nome,
                    marcaId = marcaId,
                    categoriaId = categoriaId,
                    volume = volume,
                    preco = preco,
                    imagemUrl = imagemUrl,
                    descricao = descricao,
                    estoque = estoque,
                    autor = autor,
                    emDestaque = emDestaque
                )
            ).jsonObject

            val resultado = SupabaseClient.client.postgrest.rpc("admin_criar_produto", params)
            val idProduto = jsonRpc.parseToJsonElement(resultado.data).jsonPrimitive.content
            Result.success(idProduto)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível criar o produto.", e))
        }
    }

    /** Atualiza os dados cadastrais de um produto existente (RPC "admin_atualizar_produto"). */
    suspend fun atualizarProduto(
        idProduto: String,
        nome: String,
        marcaId: String,
        categoriaId: String,
        volume: Int,
        preco: Double,
        imagemUrl: String,
        descricao: String,
        estoque: Int,
        autor: String,
        emDestaque: Boolean
    ): Result<Unit> {
        return try {
            val params = jsonRpc.encodeToJsonElement(
                AtualizarProdutoParamsDto.serializer(),
                AtualizarProdutoParamsDto(
                    idProduto = idProduto,
                    nome = nome,
                    marcaId = marcaId,
                    categoriaId = categoriaId,
                    volume = volume,
                    preco = preco,
                    imagemUrl = imagemUrl,
                    descricao = descricao,
                    estoque = estoque,
                    autor = autor,
                    emDestaque = emDestaque
                )
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("admin_atualizar_produto", params)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível atualizar o produto.", e))
        }
    }

    /** Ativa ou desativa um produto (RPC "admin_alternar_ativo_produto") — não remove do banco. */
    suspend fun alternarAtivoProduto(idProduto: String, ativo: Boolean): Result<Unit> {
        return try {
            val params = jsonRpc.encodeToJsonElement(
                AlternarAtivoProdutoParamsDto.serializer(),
                AlternarAtivoProdutoParamsDto(idProduto = idProduto, ativo = ativo)
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("admin_alternar_ativo_produto", params)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível alterar o status do produto.", e))
        }
    }

    suspend fun criarCategoria(nome: String): Result<String> = criarComNome("admin_criar_categoria", nome, "categoria")

    suspend fun atualizarCategoria(id: String, nome: String): Result<Unit> =
        atualizarComNome("admin_atualizar_categoria", id, nome, "categoria")

    suspend fun criarMarca(nome: String): Result<String> = criarComNome("admin_criar_marca", nome, "editora")

    suspend fun atualizarMarca(id: String, nome: String): Result<Unit> =
        atualizarComNome("admin_atualizar_marca", id, nome, "editora")

    private suspend fun criarComNome(rpc: String, nome: String, rotulo: String): Result<String> {
        return try {
            val params = jsonRpc.encodeToJsonElement(NomeParamsDto.serializer(), NomeParamsDto(nome)).jsonObject
            val resultado = SupabaseClient.client.postgrest.rpc(rpc, params)
            val id = jsonRpc.parseToJsonElement(resultado.data).jsonPrimitive.content
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível criar a $rotulo.", e))
        }
    }

    private suspend fun atualizarComNome(rpc: String, id: String, nome: String, rotulo: String): Result<Unit> {
        return try {
            val params = jsonRpc.encodeToJsonElement(
                AtualizarNomeParamsDto.serializer(),
                AtualizarNomeParamsDto(id = id, nome = nome)
            ).jsonObject
            SupabaseClient.client.postgrest.rpc(rpc, params)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível atualizar a $rotulo.", e))
        }
    }

    /** Ajuste rápido de estoque (RPC "admin_ajustar_estoque"), usado na tela de Estoque. */
    suspend fun ajustarEstoque(idProduto: String, estoque: Int): Result<Unit> {
        return try {
            val params = jsonRpc.encodeToJsonElement(
                AjustarEstoqueParamsDto.serializer(),
                AjustarEstoqueParamsDto(idProduto = idProduto, estoque = estoque)
            ).jsonObject
            SupabaseClient.client.postgrest.rpc("admin_ajustar_estoque", params)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível ajustar o estoque.", e))
        }
    }
}