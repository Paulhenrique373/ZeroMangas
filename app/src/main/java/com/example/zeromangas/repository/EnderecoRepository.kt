package com.example.zeromangas.repository

import com.example.zeromangas.data.model.Endereco
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
    @SerialName("p_uf") val uf: String,
    @SerialName("p_nome_destinatario") val nomeDestinatario: String,
    @SerialName("p_telefone") val telefone: String,
    @SerialName("p_informacoes_adicionais") val informacoesAdicionais: String,
    @SerialName("p_padrao") val padrao: Boolean
)

@Serializable
private data class ListarEnderecosParamsDto(
    @SerialName("p_cliente_id") val clienteId: String
)

@Serializable
private data class EnderecoDto(
    val id: String,
    @SerialName("cliente_id") val clienteId: String,
    val cep: String,
    val logradouro: String,
    val numero: String,
    val complemento: String? = null,
    val bairro: String,
    val cidade: String,
    val uf: String,
    @SerialName("nome_destinatario") val nomeDestinatario: String? = null,
    val telefone: String? = null,
    @SerialName("informacoes_adicionais") val informacoesAdicionais: String? = null,
    val padrao: Boolean = false
)

@Serializable
private data class EditarEnderecoParamsDto(
    @SerialName("p_endereco_id") val enderecoId: String,
    @SerialName("p_cliente_id") val clienteId: String,
    @SerialName("p_cep") val cep: String,
    @SerialName("p_logradouro") val logradouro: String,
    @SerialName("p_numero") val numero: String,
    @SerialName("p_complemento") val complemento: String,
    @SerialName("p_bairro") val bairro: String,
    @SerialName("p_cidade") val cidade: String,
    @SerialName("p_uf") val uf: String,
    @SerialName("p_nome_destinatario") val nomeDestinatario: String,
    @SerialName("p_telefone") val telefone: String,
    @SerialName("p_informacoes_adicionais") val informacoesAdicionais: String
)

@Serializable
private data class ExcluirEnderecoParamsDto(
    @SerialName("p_endereco_id") val enderecoId: String,
    @SerialName("p_cliente_id") val clienteId: String
)

@Serializable
private data class DefinirEnderecoPadraoParamsDto(
    @SerialName("p_endereco_id") val enderecoId: String,
    @SerialName("p_cliente_id") val clienteId: String
)

private val jsonRpc = Json { encodeDefaults = true }

private fun EnderecoDto.paraModel(): Endereco = Endereco(
    id = id,
    clienteId = clienteId,
    nomeDestinatario = nomeDestinatario ?: "",
    telefone = telefone ?: "",
    cep = cep,
    uf = uf,
    cidade = cidade,
    bairro = bairro,
    logradouro = logradouro,
    numero = numero,
    complemento = complemento ?: "",
    informacoesAdicionais = informacoesAdicionais ?: "",
    padrao = padrao
)

class EnderecoRepository {

    /**
     * Salva um novo endereço via a função "salvar_endereco" (security definer),
     * já que a tabela "enderecos" tem RLS habilitada sem policy de insert direto.
     * Se for o primeiro endereço do cliente, o banco já marca ele como padrão
     * automaticamente, mesmo que [padrao] seja false aqui.
     */
    suspend fun salvarEndereco(
        clienteId: String,
        cep: String,
        logradouro: String,
        numero: String,
        complemento: String,
        bairro: String,
        cidade: String,
        uf: String,
        nomeDestinatario: String = "",
        telefone: String = "",
        informacoesAdicionais: String = "",
        padrao: Boolean = false
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
                    uf = uf,
                    nomeDestinatario = nomeDestinatario,
                    telefone = telefone,
                    informacoesAdicionais = informacoesAdicionais,
                    padrao = padrao
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

    /**
     * Lista os endereços do cliente (padrão primeiro, depois mais recentes),
     * via a função "listar_enderecos".
     */
    suspend fun listarEnderecos(clienteId: String): Result<List<Endereco>> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                ListarEnderecosParamsDto.serializer(),
                ListarEnderecosParamsDto(clienteId = clienteId)
            ).jsonObject

            val enderecos = SupabaseClient.client.postgrest
                .rpc("listar_enderecos", paramsJson)
                .decodeList<EnderecoDto>()
                .map { it.paraModel() }

            Result.success(enderecos)
        } catch (e: Exception) {
            Result.failure(Exception("Não foi possível carregar seus endereços.", e))
        }
    }

    /**
     * Edita um endereço existente via a função "editar_endereco".
     */
    suspend fun editarEndereco(
        enderecoId: String,
        clienteId: String,
        cep: String,
        logradouro: String,
        numero: String,
        complemento: String,
        bairro: String,
        cidade: String,
        uf: String,
        nomeDestinatario: String,
        telefone: String,
        informacoesAdicionais: String
    ): Result<Unit> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                EditarEnderecoParamsDto.serializer(),
                EditarEnderecoParamsDto(
                    enderecoId = enderecoId,
                    clienteId = clienteId,
                    cep = cep,
                    logradouro = logradouro,
                    numero = numero,
                    complemento = complemento,
                    bairro = bairro,
                    cidade = cidade,
                    uf = uf,
                    nomeDestinatario = nomeDestinatario,
                    telefone = telefone,
                    informacoesAdicionais = informacoesAdicionais
                )
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("editar_endereco", paramsJson)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Não foi possível atualizar o endereço.", e))
        }
    }

    /**
     * Exclui um endereço via a função "excluir_endereco". Se o endereço excluído
     * era o padrão, o banco promove automaticamente o mais recente restante.
     */
    suspend fun excluirEndereco(enderecoId: String, clienteId: String): Result<Unit> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                ExcluirEnderecoParamsDto.serializer(),
                ExcluirEnderecoParamsDto(enderecoId = enderecoId, clienteId = clienteId)
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("excluir_endereco", paramsJson)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Não foi possível excluir o endereço.", e))
        }
    }

    /**
     * Define um endereço como padrão via a função "definir_endereco_padrao".
     */
    suspend fun definirEnderecoPadrao(enderecoId: String, clienteId: String): Result<Unit> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                DefinirEnderecoPadraoParamsDto.serializer(),
                DefinirEnderecoPadraoParamsDto(enderecoId = enderecoId, clienteId = clienteId)
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("definir_endereco_padrao", paramsJson)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Não foi possível definir o endereço padrão.", e))
        }
    }
}