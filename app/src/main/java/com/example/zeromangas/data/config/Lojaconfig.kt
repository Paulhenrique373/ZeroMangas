package com.example.zeromangas.data.config

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Ponto local (loja física / centro de distribuição) do ZeroMangás.
 * É a origem dos pedidos: o frete (ver CartViewModel.valorFretePorUf) é
 * calculado a partir daqui. Se o endereço mudar, é só alterar neste arquivo.
 */
object LojaConfig {
    const val NOME = "ZeroMangás Liberdade"
    const val LOGRADOURO = "Av. da Liberdade"
    const val NUMERO = "776 A"
    const val BAIRRO = "Liberdade"
    const val CIDADE = "São Paulo"
    const val UF = "SP"
    const val CEP = "01502-001"

    /** Valor gravado em "tipo_frete" do pedido quando o cliente retira na loja. */
    const val TIPO_FRETE_RETIRADA = "Retirada na loja"

    /** Nome do endereço "de retirada" guardado na conta do cliente (só pra o pedido ter endereço). */
    const val NOME_DESTINATARIO_RETIRADA = "Retirada na loja"

    /** Primeira linha: "Av. da Liberdade, 776 A". */
    const val ENDERECO_LINHA_1 = "$LOGRADOURO, $NUMERO"

    /** Segunda linha: "Liberdade, São Paulo - SP · CEP 01502-001". */
    const val ENDERECO_LINHA_2 = "$BAIRRO, $CIDADE - $UF · CEP $CEP"

    /** Endereço completo em uma linha, no formato que o Google Maps entende bem. */
    const val ENDERECO_COMPLETO = "$LOGRADOURO, $NUMERO - $BAIRRO, $CIDADE - $UF, $CEP"

    /**
     * Abre o endereço da loja no Google Maps (ou em outro app de mapas). Usa a
     * busca por texto do endereço — não depende de coordenadas fixas. Se nenhum
     * app de mapas responder, cai pro Maps no navegador.
     */
    fun abrirNoMapa(context: Context) {
        val consulta = Uri.encode(ENDERECO_COMPLETO)
        val intentApp = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$consulta"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intentApp)
        } catch (_: ActivityNotFoundException) {
            val intentWeb = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$consulta")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intentWeb)
            } catch (_: ActivityNotFoundException) {
                // Sem app de mapas nem navegador: não há o que fazer, não deixa o app cair.
            }
        }
    }
}