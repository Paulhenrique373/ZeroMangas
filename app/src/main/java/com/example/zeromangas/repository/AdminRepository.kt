package com.example.zeromangas.repository

import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

/**
 * Verificações de autorização administrativa. A fonte da verdade é sempre o banco
 * (tabelas "perfis"/"usuario_perfil", checadas pela função sou_admin() no Supabase,
 * que olha o auth.uid() da sessão atual) — nunca um valor local guardado no app.
 * Qualquer tela/rota administrativa deve confirmar por aqui antes de mostrar dados
 * ou ações sensíveis; a proteção de verdade continua sendo o RLS do banco, isso
 * aqui só decide o que a interface mostra.
 */
class AdminRepository {

    suspend fun souAdmin(): Result<Boolean> {
        return try {
            val resultado = SupabaseClient.client.postgrest.rpc("sou_admin")
            Result.success(resultado.data.trim().toBooleanStrictOrNull() ?: false)
        } catch (e: Exception) {
            // Se der erro de rede/etc, trata como "não é admin" — nunca libera acesso
            // administrativo por causa de uma falha ao verificar.
            Result.failure(e)
        }
    }
}