package com.example.zeromangas.repository

import android.content.Context
import android.net.Uri
import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Responsável por enviar a foto de perfil escolhida pelo usuário
 * para o bucket "avatars" no Supabase Storage.
 *
 * A URL pública retornada deve ser salva no perfil do usuário no Supabase
 * (mesmo padrão usado hoje pro campo de foto de perfil).
 */
class StorageRepository {

    private val bucket = SupabaseClient.client.storage.from("avatars")

    /**
     * Faz upload da imagem selecionada e retorna a URL pública.
     *
     * @param context necessário para ler os bytes da URI escolhida (galeria)
     * @param uri URI da imagem selecionada pelo usuário
     * @param usuarioId usado para nomear o arquivo (evita conflito entre usuários)
     */
    suspend fun uploadFotoPerfil(context: Context, uri: Uri, usuarioId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                    val output = ByteArrayOutputStream()
                    input.copyTo(output)
                    output.toByteArray()
                } ?: return@withContext Result.failure(Exception("Não foi possível ler a imagem selecionada"))

                val nomeArquivo = "perfil_$usuarioId.jpg"

                bucket.upload(nomeArquivo, bytes) {
                    upsert = true // sobrescreve se já existir uma foto anterior desse usuário
                }

                val urlPublica = bucket.publicUrl(nomeArquivo)
                Result.success(urlPublica)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}