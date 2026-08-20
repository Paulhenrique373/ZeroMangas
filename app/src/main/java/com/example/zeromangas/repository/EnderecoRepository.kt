package com.example.zeromangas.repository

import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

@Serializable
private data class EnderecoDto(
    val id: String? = null,
    val cliente_id: String,
    val cep: String,
    val logradouro: String = "",
    val numero: String = "",
    val complemento: String = "",
    val bairro: String = "",
    val cidade: String,
    val uf: String
)

class EnderecoRepository {

    private val enderecosTable = SupabaseClient.client.postgrest.from("enderecos")

    suspend fun salvarEndereco(
        clienteId: String,
        cep: String,
        logradouro: String,
        numero: String,
        complemento: String,
        bairro: String,
        cidade: String,
        uf: String
    ): Result<String> {
        return try {
            val inserido = enderecosTable
                .insert(
                    EnderecoDto(
                        cliente_id = clienteId,
                        cep = cep,
                        logradouro = logradouro,
                        numero = numero,
                        complemento = complemento,
                        bairro = bairro,
                        cidade = cidade,
                        uf = uf
                    )
                ) { select() }
                .decodeSingle<EnderecoDto>()

            Result.success(inserido.id ?: return Result.failure(Exception("Falha ao salvar endereço")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}