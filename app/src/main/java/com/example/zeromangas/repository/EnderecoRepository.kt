package com.example.zeromangas.repository

import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@Serializable
private data class SalvarEnderecoParamsDto(
    @SerialName("p_cliente_id") val clienteId: String,
    @SerialName("p_cep") val cep: String,
    @SerialName("p_logradouro") val logradouro: String,
    @SerialName("p_numero") val numero: String,
    @SerialName("p_complemento") val complemento: String,
    @SerialName("p_bairro") val bairro: String,
    @SerialName("p_cidade") val cidade: String,
    @SerialName("p_uf") val uf: String
)

private val jsonRpc = Json { encodeDefaults = true }

class EnderecoRepository {

    /**
     * Salva o endereço via a função "salvar_endereco" (security definer),
     * já que a tabela "enderecos" tem RLS habilitada sem policy de insert direto.
     */
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
            val paramsJson = jsonRpc.encodeToJsonElement(
                SalvarEnderecoParamsDto.serializer(),
                SalvarEnderecoParamsDto(
                    clienteId = clienteId,
                    cep = cep,
                    logradouro = logradouro,
                    numero = numero,
                    complemento = complemento,
                    bairro = bairro,
                    cidade = cidade,
                    uf = uf
                )
            ).jsonObject

            val resultado = SupabaseClient.client.postgrest.rpc("salvar_endereco", paramsJson)
            val enderecoId = resultado.data.trim('"')

            if (enderecoId.isBlank() || enderecoId == "null") {
                Result.failure(Exception("Falha ao salvar endereço"))
            } else {
                Result.success(enderecoId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}