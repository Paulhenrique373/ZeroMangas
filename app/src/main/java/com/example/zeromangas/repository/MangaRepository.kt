package com.example.zeromangas.repository

import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.data.remote.CategoriaDto
import com.example.zeromangas.data.remote.MarcaDto
import com.example.zeromangas.data.remote.ProdutoDto
import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class EstoqueDto(
    @SerialName("id_produto") val id: String,
    val estoque: Int
)

@Serializable
private data class AutorDto(
    val autor: String? = null
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
    estoque = estoque,
    autor = autor ?: "",
    notaMedia = notaMedia,
    totalAvaliacoes = totalAvaliacoes,
    precoPromocional = precoPromocional,
    emPromocao = emPromocao
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
     * Lista os autores distintos cadastrados nos produtos, pro filtro de "autor"
     * da Busca. Não existe uma tabela "autores" separada — é só uma coluna de
     * texto em "produtos" — então a deduplicação é feita aqui mesmo.
     */
    suspend fun listarAutores(): Result<List<String>> {
        return try {
            val dtos = produtosTable
                .select(columns = Columns.list("autor"))
                .decodeList<AutorDto>()

            val autores = dtos.mapNotNull { it.autor?.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()

            Result.success(autores)
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
                        eq("id_produto", id)
                    }
                }
                .decodeList<ProdutoDto>()
                .firstOrNull() ?: return Result.failure(Exception("Mangá não encontrado."))

            val manga = dto.paraManga()

            val recomendadosDtos = produtosTable
                .select(columns = Columns.raw("*, marcas(nome), categorias(nome)")) {
                    filter {
                        eq("id_categoria", dto.categoriaId)
                        neq("id_produto", id)
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
                .select(columns = Columns.list("id_produto", "estoque")) {
                    filter {
                        isIn("id_produto", produtoIds)
                    }
                }
                .decodeList<EstoqueDto>()

            Result.success(dtos.associate { it.id to it.estoque })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}