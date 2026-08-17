package com.example.zeromangas.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * Cliente único do Supabase, usado para Storage (upload de fotos de perfil)
 * e Postgrest (banco de dados relacional: produtos, pedidos, favoritos, cupons).
 * A autenticação continua no Firebase Auth.
 */
object SupabaseClient {

    private const val SUPABASE_URL = "https://znqceiplzfeexbjkgebm.supabase.co"
    private const val SUPABASE_ANON_KEY = "sb_publishable_3YPPorC9cqB8jqN8hDLagA_-aXjOpYV"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Storage)
        install(Postgrest)
    }
}