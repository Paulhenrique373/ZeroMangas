package com.example.zeromangas.repository

import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class UsuarioDto(
    val id: String? = null,
    @SerialName("firebase_uid") val firebaseUid: String,
    val nome: String,
    val email: String
)

@Serializable
private data class ClienteDto(
    val id: String? = null,
    @SerialName("usuario_id") val usuarioId: String
)

/**
 * Sincroniza o usuário logado no Firebase com as tabelas "usuarios" e "clientes"
 * do Supabase. Deve ser chamado após cadastro e após login bem-sucedidos,
 * já que o Postgres não tem visibilidade automática de quem loga via Firebase Auth.
 */
class UsuarioRepository {

    private val usuariosTable = SupabaseClient.client.postgrest.from("usuarios")
    private val clientesTable = SupabaseClient.client.postgrest.from("clientes")

    suspend fun sincronizarUsuario(firebaseUid: String, nome: String, email: String): Result<Unit> {
        return try {
            // Verifica se o usuário já existe (idempotente: seguro chamar a cada login)
            val existentes = usuariosTable
                .select {
                    filter { eq("firebase_uid", firebaseUid) }
                }
                .decodeList<UsuarioDto>()

            val usuarioId: String

            if (existentes.isEmpty()) {
                val inserido = usuariosTable
                    .insert(UsuarioDto(firebaseUid = firebaseUid, nome = nome, email = email)) {
                        select()
                    }
                    .decodeSingle<UsuarioDto>()
                usuarioId = inserido.id ?: return Result.failure(Exception("Falha ao criar usuário"))

                // Cria o registro de cliente 1:1 junto, só na primeira vez
                clientesTable.insert(ClienteDto(usuarioId = usuarioId))
            } else {
                usuarioId = existentes.first().id ?: return Result.failure(Exception("Usuário sem id"))

                // Mantém nome/email atualizados caso o usuário edite o perfil no Firebase
                usuariosTable.update(
                    UsuarioDto(firebaseUid = firebaseUid, nome = nome, email = email)
                ) {
                    filter { eq("firebase_uid", firebaseUid) }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Busca o id do cliente (tabela "clientes") a partir do UID do Firebase,
     * passando por "usuarios" no meio (usuarios.firebase_uid -> usuarios.id -> clientes.usuario_id).
     */
    suspend fun buscarClienteId(firebaseUid: String): Result<String> {
        return try {
            val usuario = usuariosTable
                .select {
                    filter { eq("firebase_uid", firebaseUid) }
                }
                .decodeList<UsuarioDto>()
                .firstOrNull() ?: return Result.failure(Exception("Usuário não sincronizado"))

            val usuarioId = usuario.id ?: return Result.failure(Exception("Usuário sem id"))

            val cliente = clientesTable
                .select {
                    filter { eq("usuario_id", usuarioId) }
                }
                .decodeList<ClienteDto>()
                .firstOrNull() ?: return Result.failure(Exception("Cliente não encontrado"))

            Result.success(cliente.id ?: return Result.failure(Exception("Cliente sem id")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}