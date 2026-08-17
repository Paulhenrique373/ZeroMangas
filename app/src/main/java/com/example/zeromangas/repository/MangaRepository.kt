package com.example.zeromangas.repository

import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.data.remote.CategoriaDto
import com.example.zeromangas.data.remote.MarcaDto
import com.example.zeromangas.data.remote.ProdutoDto
import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

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
}