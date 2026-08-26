package com.example.zeromangas.data.remote

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Cliente único do Supabase, usado para Auth (login/cadastro/sessão), Storage
 * (upload de fotos de perfil), Postgrest (banco de dados relacional: produtos,
 * pedidos, favoritos, cupons) e Realtime (notificações ao vivo: promoção/estoque
 * de favoritos). O Firebase saiu do projeto — agora é 100% Supabase.
 *
 * A URL e a chave abaixo ficam hardcoded de propósito: é a chave "publishable"
 * do Supabase, feita para ser exposta no client (equivalente à chave pública
 * do Firebase) — não é um segredo que precise ir para local.properties/BuildConfig.
 */
object SupabaseClient {

    private const val SUPABASE_URL = "https://znqceiplzfeexbjkgebm.supabase.co"
    private const val SUPABASE_ANON_KEY = "sb_publishable_3YPPorC9cqB8jqN8hDLagA_-aXjOpYV"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Storage)
        install(Postgrest)
        install(Realtime)
    }
}