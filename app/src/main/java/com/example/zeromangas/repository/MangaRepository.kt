package com.example.zeromangas.repository

import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.data.remote.CategoriaDto
import com.example.zeromangas.data.remote.MarcaDto
import com.example.zeromangas.data.remote.ProdutoDto
import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable

@Serializable
private data class EstoqueDto(
    val id: String,
    val estoque: Int
)

@Serializable
private data class EstoqueUpdateDto(
    val estoque: Int
)

class MangaRepository {

    private val produtosTable = SupabaseClient.client.postgrest.from("produtos")
    private val categoriasTable = SupabaseClient.client.postgrest.from("categorias")
    private val marcasTable = SupabaseClient.client.postgrest.from("marcas")

    /**
     * Busca todos os produtos no Supabase, já trazendo o nome da marca e da categoria
     * através do join automático do Postgrest (marca_id -> marcas, categoria_id -> categorias).
     */
    suspend fun listarMangas(): Result<List<Manga>> {
        return try {
            val dtos = produtosTable
                .select(columns = Columns.raw("*, marcas(nome), categorias(nome)"))
                .decodeList<ProdutoDto>()

            val mangas = dtos.map { dto ->
                Manga(
                    id = dto.id,
                    nome = dto.nome,
                    marca = dto.marcas?.nome ?: "",
                    categoria = dto.categorias?.nome ?: "",
                    volume = dto.volume,
                    preco = dto.preco,
                    imagemUrl = dto.imagemUrl,
                    descricao = dto.descricao,
                    emDestaque = dto.emDestaque,
                    estoque = dto.estoque
                )
            }
            Result.success(mangas)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listarCategorias(): Result<List<String>> {
        return try {
            val lista = categoriasTable.select().decodeList<CategoriaDto>()
            Result.success(lista.map { it.nome }.sorted())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listarMarcas(): Result<List<String>> {
        return try {
            val lista = marcasTable.select().decodeList<MarcaDto>()
            Result.success(lista.map { it.nome }.sorted())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Busca o estoque atual (direto do banco) dos produtos informados.
     * Usado no checkout para validar a compra contra o estoque real,
     * em vez de confiar no valor que já estava carregado na tela.
     */
    suspend fun buscarEstoqueAtual(produtoIds: List<String>): Result<Map<String, Int>> {
        if (produtoIds.isEmpty()) {
            return Result.success(emptyMap())
        }

        return try {
            val dtos = produtosTable
                .select(columns = Columns.list("id", "estoque")) {
                    filter {
                        isIn("id", produtoIds)
                    }
                }
                .decodeList<EstoqueDto>()

            Result.success(dtos.associate { it.id to it.estoque })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Desconta a quantidade comprada do estoque de um produto após a compra ser confirmada.
     * O filtro por "estoque = estoqueAtual" evita que duas compras simultâneas
     * descontem o mesmo estoque duas vezes (proteção básica contra condição de corrida).
     */
    suspend fun descontarEstoque(
        produtoId: String,
        quantidadeComprada: Int,
        estoqueAtual: Int
    ): Result<Unit> {
        return try {
            val novoEstoque = (estoqueAtual - quantidadeComprada).coerceAtLeast(0)

            produtosTable.update(EstoqueUpdateDto(estoque = novoEstoque)) {
                filter {
                    eq("id", produtoId)
                    eq("estoque", estoqueAtual)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}