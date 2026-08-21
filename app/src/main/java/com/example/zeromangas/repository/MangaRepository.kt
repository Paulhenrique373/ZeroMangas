package com.example.zeromangas.repository

import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.data.remote.CategoriaDto
import com.example.zeromangas.data.remote.MarcaDto
import com.example.zeromangas.data.remote.ProdutoDto
import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class EstoqueDto(
    val id: String,
    val estoque: Int
)

/** Converte o DTO vindo do Supabase (com os joins de marca/categoria) para o model de UI. */
private fun ProdutoDto.paraManga(): Manga = Manga(
    id = id,
    nome = nome,
    marca = marcas?.nome ?: "",
    categoria = categorias?.nome ?: "",
    volume = volume,
    preco = preco,
    imagemUrl = imagemUrl,
    descricao = descricao,
    emDestaque = emDestaque,
    estoque = estoque
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

            Result.success(dtos.map { it.paraManga() })
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
     * Busca um único produto pelo id (usado na tela de Detalhes), em vez de baixar o
     * catálogo inteiro só para filtrar um item em memória. Já retorna junto até
     * [limiteRecomendados] produtos da mesma categoria (excluindo o próprio produto),
     * filtrando por "categoria_id" direto no banco.
     */
    suspend fun buscarMangaComRecomendados(
        id: String,
        limiteRecomendados: Int = 10
    ): Result<Pair<Manga, List<Manga>>> {
        return try {
            val dto = produtosTable
                .select(columns = Columns.raw("*, marcas(nome), categorias(nome)")) {
                    filter {
                        eq("id", id)
                    }
                }
                .decodeList<ProdutoDto>()
                .firstOrNull() ?: return Result.failure(Exception("Mangá não encontrado."))

            val manga = dto.paraManga()

            val recomendadosDtos = produtosTable
                .select(columns = Columns.raw("*, marcas(nome), categorias(nome)")) {
                    filter {
                        eq("categoria_id", dto.categoriaId)
                        neq("id", id)
                    }
                }
                .decodeList<ProdutoDto>()

            val recomendados = recomendadosDtos.map { it.paraManga() }.take(limiteRecomendados)

            Result.success(manga to recomendados)
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
     * Chama a função "descontar_estoque" no banco (RPC), que faz a checagem e o desconto
     * numa única operação atômica: só desconta se ainda houver estoque suficiente naquele
     * instante, eliminando a janela de tempo entre "ler o estoque" e "gravar o desconto"
     * (o que antes permitia duas compras simultâneas descontarem o mesmo item indevidamente).
     * Retorna o estoque restante em caso de sucesso, ou falha com "ESTOQUE_INSUFICIENTE"
     * se o estoque tiver acabado entre a validação e este momento.
     */
    suspend fun descontarEstoque(
        produtoId: String,
        quantidadeComprada: Int
    ): Result<Int> {
        return try {
            val parametros = buildJsonObject {
                put("p_produto_id", produtoId)
                put("p_quantidade", quantidadeComprada)
            }

            val estoqueRestante = SupabaseClient.client.postgrest
                .rpc("descontar_estoque", parametros)
                .decodeAs<Int>()

            Result.success(estoqueRestante)
        } catch (e: Exception) {
            if (e.message?.contains("ESTOQUE_INSUFICIENTE") == true) {
                Result.failure(Exception("ESTOQUE_INSUFICIENTE"))
            } else {
                Result.failure(e)
            }
        }
    }
}