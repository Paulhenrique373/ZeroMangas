package com.example.zeromangas.ui.theme.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zeromangas.repository.AdminPedidosRepository
import com.example.zeromangas.ui.components.formatarPrecoBr
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.RoxoNeonClaro
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.VerdeSucesso
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class VendaDia(val dia: LocalDate, val total: Double, val pedidos: Int)

data class TopManga(val nome: String, val quantidade: Int, val receita: Double)

data class RelatorioVendas(
    val porDia: List<VendaDia>,
    val topMangas: List<TopManga>,
    val diasNoGrafico: Int
)

// Só pedidos pagos entram no relatório (PENDENTE e CANCELADO ficam de fora).
private val STATUS_VENDA = setOf("PAGAMENTO_APROVADO", "EM_PREPARACAO", "ENVIADO", "ENTREGUE")

// Limite de pedidos consultados pra montar o ranking (1 chamada por pedido).
private const val LIMITE_PEDIDOS_RANKING = 60

private fun dataLocal(texto: String?): LocalDate? {
    if (texto.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(texto).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
    } catch (e: Exception) {
        try { LocalDate.parse(texto.take(10)) } catch (e2: Exception) { null }
    }
}

/**
 * Monta o relatório do Dashboard usando só as RPCs admin que já existem
 * (admin_listar_pedidos e admin_listar_itens_pedido), então não precisa de SQL novo.
 * - Vendas por dia: últimos [dias] dias (mínimo 7, máximo 14 barras).
 * - Top mangás: itens dos pedidos pagos do período, somando quantidade vendida.
 */
suspend fun carregarRelatorioVendas(
    repo: AdminPedidosRepository,
    dias: Int
): Result<RelatorioVendas> {
    return try {
        val pedidos = repo.listarPedidos("").getOrThrow()
        val hoje = LocalDate.now()
        val diasGrafico = dias.coerceIn(7, 14)

        val pagos = pedidos
            .filter { it.status.uppercase() in STATUS_VENDA }
            .mapNotNull { p -> dataLocal(p.dataPedido)?.let { it to p } }

        val porDia = (diasGrafico - 1 downTo 0).map { atras ->
            val dia = hoje.minusDays(atras.toLong())
            val doDia = pagos.filter { it.first == dia }
            VendaDia(dia, doDia.sumOf { it.second.valorTotal }, doDia.size)
        }

        val inicioPeriodo = hoje.minusDays((dias - 1).toLong())
        val doPeriodo = pagos
            .filter { !it.first.isBefore(inicioPeriodo) }
            .sortedByDescending { it.first }
            .take(LIMITE_PEDIDOS_RANKING)

        val itens = coroutineScope {
            doPeriodo.map { (_, p) -> async { repo.listarItens(p.idPedido).getOrNull().orEmpty() } }
                .awaitAll()
                .flatten()
        }

        val top = itens
            .groupBy { it.produtoId }
            .map { (_, lista) ->
                TopManga(
                    nome = lista.firstNotNullOfOrNull { it.produtoNome } ?: "Mangá",
                    quantidade = lista.sumOf { it.quantidade },
                    receita = lista.sumOf { it.precoUnitario * it.quantidade }
                )
            }
            .sortedByDescending { it.quantidade }
            .take(5)

        Result.success(RelatorioVendas(porDia, top, diasGrafico))
    } catch (e: Exception) {
        Result.failure(Exception(e.message ?: "Não foi possível carregar os gráficos.", e))
    }
}

/** Seção do Dashboard: gráfico de vendas por dia + ranking dos mangás mais vendidos. */
@Composable
fun SecaoRelatorioVendas(relatorio: RelatorioVendas?, carregando: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        when {
            carregando -> Surface(color = FundoCard, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RoxoNeon)
                }
            }
            relatorio == null -> Text(
                "Não foi possível carregar os gráficos.",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )
            else -> {
                GraficoVendasPorDia(relatorio)
                RankingTopMangas(relatorio.topMangas)
            }
        }
    }
}

@Composable
private fun GraficoVendasPorDia(relatorio: RelatorioVendas) {
    val dados = relatorio.porDia
    val maximo = dados.maxOfOrNull { it.total } ?: 0.0
    val melhor = dados.maxByOrNull { it.total }
    val formatoDia = DateTimeFormatter.ofPattern("dd/MM")

    Surface(color = FundoCard, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text("Vendas por dia", style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
            Text(
                "Últimos ${relatorio.diasNoGrafico} dias · ${formatarPrecoBr(dados.sumOf { it.total })}",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )
            Spacer(Modifier.height(Spacing.md))

            if (maximo <= 0.0) {
                Text(
                    "Nenhuma venda nesse intervalo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    dados.forEach { d ->
                        val fracao = (d.total / maximo).toFloat().coerceIn(0f, 1f)
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (d.total > 0) (120.dp * fracao).coerceAtLeast(3.dp) else 2.dp)
                                    .background(
                                        if (d.dia == melhor?.dia) VerdeSucesso else RoxoNeon,
                                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    dados.forEach { d ->
                        Text(
                            text = d.dia.format(formatoDia).take(2),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp,
                            color = TextoSecundario,
                            maxLines = 1
                        )
                    }
                }
                if (melhor != null && melhor.total > 0) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        "Melhor dia: ${melhor.dia.format(formatoDia)} · ${formatarPrecoBr(melhor.total)} (${melhor.pedidos} pedido(s))",
                        style = MaterialTheme.typography.bodySmall,
                        color = VerdeSucesso
                    )
                }
            }
        }
    }
}

@Composable
private fun RankingTopMangas(top: List<TopManga>) {
    Surface(color = FundoCard, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text("Mangás mais vendidos", style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
            if (top.isEmpty()) {
                Text(
                    "Nenhuma venda no período selecionado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            } else {
                val maior = top.first().quantidade.coerceAtLeast(1)
                top.forEachIndexed { i, m ->
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${i + 1}. ${m.nome}",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextoPrincipal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${m.quantidade} un.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RoxoNeonClaro
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(TextoSecundario.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(m.quantidade.toFloat() / maior)
                                    .fillMaxHeight()
                                    .background(RoxoNeon, RoundedCornerShape(3.dp))
                            )
                        }
                        Text(
                            formatarPrecoBr(m.receita),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSecundario
                        )
                    }
                }
            }
        }
    }
}