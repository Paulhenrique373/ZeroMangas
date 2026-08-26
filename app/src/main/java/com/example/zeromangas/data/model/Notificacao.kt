package com.example.zeromangas.data.model

enum class TipoNotificacao {
    PROMOCAO, LANCAMENTO, PEDIDO, FAVORITO_PROMOCAO, FAVORITO_ESTOQUE
}

fun String.paraTipoNotificacao(): TipoNotificacao = when (this) {
    "promocao" -> TipoNotificacao.PROMOCAO
    "lancamento" -> TipoNotificacao.LANCAMENTO
    "pedido" -> TipoNotificacao.PEDIDO
    "favorito_promocao" -> TipoNotificacao.FAVORITO_PROMOCAO
    "favorito_estoque" -> TipoNotificacao.FAVORITO_ESTOQUE
    else -> TipoNotificacao.PROMOCAO
}

data class Notificacao(
    val id: String = "",
    val tipo: TipoNotificacao = TipoNotificacao.PROMOCAO,
    val titulo: String = "",
    val mensagem: String = "",
    val produtoId: String? = null,
    val pedidoId: String? = null,
    val lida: Boolean = false,
    val criadoEm: String = ""
)