package com.example.zeromangas.data.local

import android.content.Context

/**
 * "Vistos recentemente": guarda no aparelho (SharedPreferences) os ids dos últimos
 * mangás cujos detalhes foram abertos. Não usa backend — só o id fica salvo; a Home
 * resolve o mangá completo a partir do catálogo já carregado.
 */
object VistosRecentemente {

    private const val ARQUIVO = "vistos_recentemente"
    private const val CHAVE_IDS = "ids"
    private const val SEPARADOR = ","
    private const val LIMITE = 10

    /** Ids dos mangás vistos, do mais recente para o mais antigo. */
    fun listar(context: Context): List<String> {
        val bruto = context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
            .getString(CHAVE_IDS, "")
            .orEmpty()
        return bruto.split(SEPARADOR).filter { it.isNotBlank() }
    }

    /** Registra [mangaId] como o mais recente (sem duplicar) e mantém só os últimos [LIMITE]. */
    fun registrar(context: Context, mangaId: String) {
        if (mangaId.isBlank()) return
        val atualizada = (listOf(mangaId) + listar(context).filter { it != mangaId }).take(LIMITE)
        context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
            .edit()
            .putString(CHAVE_IDS, atualizada.joinToString(SEPARADOR))
            .apply()
    }

    fun limpar(context: Context) {
        context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit().remove(CHAVE_IDS).apply()
    }
}