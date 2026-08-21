package com.example.zeromangas.repository

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

private val jsonRpc = Json { encodeDefaults = true }

/**
 * Sincroniza o usuário logado no Firebase com as tabelas "usuarios", "clientes" e
 * "usuario_perfil" do Supabase. Deve ser chamado após cadastro e após login
 * bem-sucedidos, já que o Postgres não tem visibilidade automática de quem loga
 * via Firebase Auth.
 *
 * Todas as operações passam pela função "sincronizar_usuario" (security definer)
 * em vez de inserts/updates diretos nas tabelas, porque elas têm RLS habilitada
 * sem policy — só a função consegue escrever nelas.
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
}