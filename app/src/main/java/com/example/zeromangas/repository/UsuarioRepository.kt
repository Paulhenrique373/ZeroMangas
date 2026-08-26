package com.example.zeromangas.repository

import com.example.zeromangas.data.model.PerfilCliente
import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@Serializable
private data class SincronizarUsuarioParamsDto(
    @SerialName("p_firebase_uid") val firebaseUid: String,
    @SerialName("p_nome") val nome: String,
    @SerialName("p_email") val email: String
)

@Serializable
private data class SincronizarUsuarioResultDto(
    @SerialName("usuario_id") val usuarioId: String,
    @SerialName("cliente_id") val clienteId: String? = null
)

@Serializable
private data class BuscarClienteIdParamsDto(
    @SerialName("p_firebase_uid") val firebaseUid: String
)

@Serializable
private data class BuscarPerfilCompletoParamsDto(
    @SerialName("p_firebase_uid") val firebaseUid: String
)

@Serializable
private data class PerfilCompletoDto(
    @SerialName("usuario_id") val usuarioId: String,
    @SerialName("cliente_id") val clienteId: String? = null,
    val nome: String? = null,
    val email: String? = null,
    @SerialName("foto_url") val fotoUrl: String? = null,
    val telefone: String? = null,
    val cpf: String? = null,
    val bio: String? = null,
    val genero: String? = null,
    @SerialName("data_nascimento") val dataNascimento: String? = null
)

@Serializable
private data class AtualizarPerfilCompletoParamsDto(
    @SerialName("p_firebase_uid") val firebaseUid: String,
    @SerialName("p_nome") val nome: String? = null,
    @SerialName("p_email") val email: String? = null,
    @SerialName("p_foto_url") val fotoUrl: String? = null,
    @SerialName("p_telefone") val telefone: String? = null,
    @SerialName("p_cpf") val cpf: String? = null,
    @SerialName("p_bio") val bio: String? = null,
    @SerialName("p_genero") val genero: String? = null,
    @SerialName("p_data_nascimento") val dataNascimento: String? = null
)

// encodeDefaults = false aqui é proposital: nos updates, um campo null significa
// "não mexe nesse valor" (ver coalesce na função do banco). Se mandássemos os
// defaults, o Postgrest enviaria "null" explícito pra tudo que a tela não editou
// e a RPC apagaria os campos não tocados — records tem que omitir o que não veio.
private val jsonRpc = Json { encodeDefaults = true }
private val jsonRpcOmitindoNulos = Json { encodeDefaults = false; explicitNulls = false }

/**
 * Sincroniza o usuário logado no Firebase com as tabelas "usuarios", "clientes" e
 * "usuario_perfil" do Supabase. Deve ser chamado após cadastro e após login
 * bem-sucedidos, já que o Postgres não tem visibilidade automática de quem loga
 * via Firebase Auth.
 *
 * Todas as operações passam por funções RPC (security definer) em vez de
 * inserts/updates diretos nas tabelas, porque elas têm RLS habilitada sem
 * policy — só as funções conseguem escrever nelas.
 */
class UsuarioRepository {

    suspend fun sincronizarUsuario(firebaseUid: String, nome: String, email: String): Result<Unit> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                SincronizarUsuarioParamsDto.serializer(),
                SincronizarUsuarioParamsDto(firebaseUid = firebaseUid, nome = nome, email = email)
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("sincronizar_usuario", paramsJson)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Busca o id do cliente (tabela "clientes") a partir do UID do Firebase,
     * via a função "buscar_cliente_id".
     */
    suspend fun buscarClienteId(firebaseUid: String): Result<String> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                BuscarClienteIdParamsDto.serializer(),
                BuscarClienteIdParamsDto(firebaseUid = firebaseUid)
            ).jsonObject

            val resultado = SupabaseClient.client.postgrest.rpc("buscar_cliente_id", paramsJson)

            val bruto = resultado.data.trim('"')
            if (bruto.isBlank() || bruto == "null") {
                Result.failure(Exception("Cliente não encontrado"))
            } else {
                Result.success(bruto)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Busca o perfil completo (usuarios + clientes) pra pré-carregar a tela
     * de Editar Perfil, via a função "buscar_perfil_completo".
     */
    suspend fun buscarPerfilCompleto(firebaseUid: String): Result<PerfilCliente> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                BuscarPerfilCompletoParamsDto.serializer(),
                BuscarPerfilCompletoParamsDto(firebaseUid = firebaseUid)
            ).jsonObject

            val dto = SupabaseClient.client.postgrest
                .rpc("buscar_perfil_completo", paramsJson)
                .decodeSingle<PerfilCompletoDto>()

            Result.success(
                PerfilCliente(
                    usuarioId = dto.usuarioId,
                    clienteId = dto.clienteId ?: "",
                    nome = dto.nome ?: "",
                    email = dto.email ?: "",
                    fotoUrl = dto.fotoUrl ?: "",
                    telefone = dto.telefone ?: "",
                    cpf = dto.cpf ?: "",
                    bio = dto.bio ?: "",
                    genero = dto.genero ?: "",
                    dataNascimento = dto.dataNascimento ?: ""
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Não foi possível carregar o perfil.", e))
        }
    }

    /**
     * Atualiza nome/e-mail (tabela "usuarios") e os campos estendidos
     * (tabela "clientes") de uma vez, via "atualizar_perfil_completo".
     * Campos null são ignorados no banco (mantém o valor atual) — por isso
     * usamos um Json que omite nulos ao serializar os parâmetros.
     */
    suspend fun atualizarPerfilCompleto(
        firebaseUid: String,
        nome: String? = null,
        email: String? = null,
        fotoUrl: String? = null,
        telefone: String? = null,
        cpf: String? = null,
        bio: String? = null,
        genero: String? = null,
        dataNascimento: String? = null
    ): Result<Unit> {
        return try {
            val paramsJson = jsonRpcOmitindoNulos.encodeToJsonElement(
                AtualizarPerfilCompletoParamsDto.serializer(),
                AtualizarPerfilCompletoParamsDto(
                    firebaseUid = firebaseUid,
                    nome = nome,
                    email = email,
                    fotoUrl = fotoUrl,
                    telefone = telefone,
                    cpf = cpf,
                    bio = bio,
                    genero = genero,
                    dataNascimento = dataNascimento
                )
            ).jsonObject

            SupabaseClient.client.postgrest.rpc("atualizar_perfil_completo", paramsJson)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Não foi possível salvar o perfil.", e))
        }
    }
}