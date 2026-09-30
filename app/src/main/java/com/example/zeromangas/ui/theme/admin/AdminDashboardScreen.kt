package com.example.zeromangas.ui.theme.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.zeromangas.repository.AdminRepository
import com.example.zeromangas.repository.DashboardExtrasDto
import com.example.zeromangas.repository.DashboardResumoDto
import com.example.zeromangas.ui.components.EmptyState
import com.example.zeromangas.ui.components.LoadingState
import com.example.zeromangas.ui.components.formatarPrecoBr
import com.example.zeromangas.ui.theme.AmareloDestaque
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.VerdeSucesso
import com.example.zeromangas.ui.theme.VermelhoErro
import java.util.Calendar

private data class OpcaoPeriodo(val label: String, val dias: Int)

private val opcoesPeriodo = listOf(
    OpcaoPeriodo("Hoje", 1),
    OpcaoPeriodo("7 dias", 7),
    // "Este mês" = do dia 1 até hoje, expresso em dias pra reaproveitar a RPC existente.
    OpcaoPeriodo("Este mês", Calendar.getInstance().get(Calendar.DAY_OF_MONTH)),
    OpcaoPeriodo("30 dias", 30),
    OpcaoPeriodo("90 dias", 90),
    OpcaoPeriodo("1 ano", 365)
)

/**
 * Dashboard real do Painel Administrativo (item 1 do pedido original).
 * Números vêm da RPC "admin_dashboard_resumo", que reconfirma sou_admin() no
 * banco antes de calcular qualquer coisa — a proteção de acesso não depende
 * só da tela em volta (ver composable(Tela.Admin.rota) no NavGraph).
 */
@Composable
fun AdminDashboardScreen(
    adminRepository: AdminRepository,
    onVoltar: () -> Unit,
    onProdutosClick: () -> Unit = {},
    onCategoriasClick: () -> Unit = {},
    onMarcasClick: () -> Unit = {},
    onEstoqueClick: () -> Unit = {},
    onPedidosClick: () -> Unit = {},
    onClientesClick: () -> Unit = {},
    onCuponsClick: () -> Unit = {}
) {
    var periodoSelecionado by remember { mutableStateOf(opcoesPeriodo[3]) } // padrão: 30 dias
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<String?>(null) }
    var resumo by remember { mutableStateOf<DashboardResumoDto?>(null) }
    var extras by remember { mutableStateOf<DashboardExtrasDto?>(null) }

    // Totais/pendências atuais: não dependem do período. Se falharem, o Dashboard
    // segue funcionando só com os cards do período.
    LaunchedEffect(Unit) {
        adminRepository.buscarExtrasDashboard().onSuccess { extras = it }
    }

    LaunchedEffect(periodoSelecionado) {
        carregando = true
        erro = null
        adminRepository.buscarResumoDashboard(periodoSelecionado.dias)
            .onSuccess { resumo = it }
            .onFailure { erro = it.message ?: "Não foi possível carregar o dashboard." }
        carregando = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.screenHorizontal),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoltar) {
                Icon(Icons.Default.ArrowBack, "Voltar", tint = TextoPrincipal)
            }
            Spacer(Modifier.width(Spacing.sm))
            Text("Painel Administrativo", style = MaterialTheme.typography.headlineSmall, color = TextoPrincipal)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            opcoesPeriodo.forEach { opcao ->
                FilterChip(
                    selected = opcao == periodoSelecionado,
                    onClick = { periodoSelecionado = opcao },
                    label = { Text(opcao.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RoxoNeon,
                        selectedLabelColor = TextoPrincipal
                    )
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            AtalhoCard("Produtos", Icons.Default.Inventory2, onProdutosClick)
            AtalhoCard("Categorias", Icons.AutoMirrored.Filled.List, onCategoriasClick)
            AtalhoCard("Editoras", Icons.Default.Sell, onMarcasClick)
            AtalhoCard("Estoque", Icons.Default.Warehouse, onEstoqueClick)
            AtalhoCard("Pedidos", Icons.Default.ShoppingBag, onPedidosClick)
            AtalhoCard("Clientes", Icons.Default.People, onClientesClick)
            AtalhoCard("Cupons", Icons.Default.ConfirmationNumber, onCuponsClick)
        }

        Spacer(Modifier.height(Spacing.md))

        when {
            carregando -> LoadingState(modifier = Modifier.weight(1f))
            erro != null -> EmptyState(
                titulo = "Não foi possível carregar",
                subtitulo = erro,
                modifier = Modifier.weight(1f)
            )
            resumo != null -> DashboardConteudo(resumo!!, extras, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DashboardConteudo(
    resumo: DashboardResumoDto,
    extras: DashboardExtrasDto?,
    modifier: Modifier = Modifier
) {
    val alertas = buildList {
        if (extras != null && extras.pedidosPendentes > 0) {
            add("⚠️ " + contagem(extras.pedidosPendentes, "pedido aguarda", "pedidos aguardam") + " pagamento.")
        }
        if (extras != null && extras.pedidosEmPreparacao > 0) {
            add("⚠️ " + contagem(extras.pedidosEmPreparacao, "pedido precisa ser enviado.", "pedidos precisam ser enviados."))
        }
        if (resumo.produtosEstoqueBaixo > 0) {
            add("⚠️ " + contagem(resumo.produtosEstoqueBaixo, "produto está", "produtos estão") + " com estoque baixo.")
        }
        if (resumo.produtosEsgotados > 0) {
            add("🔴 " + contagem(resumo.produtosEsgotados, "produto está", "produtos estão") + " sem estoque.")
        }
    }

    val cards = buildList {
        add(CardMetrica("Faturamento", formatarPrecoBr(resumo.faturamentoTotal), VerdeSucesso))
        add(CardMetrica("Pedidos", resumo.totalPedidos.toString(), RoxoNeon))
        add(CardMetrica("Ticket médio", formatarPrecoBr(resumo.ticketMedio), RoxoNeon))
        add(CardMetrica("Novos clientes", resumo.novosClientes.toString(), RoxoNeon))
        add(CardMetrica("Cancelados", resumo.pedidosCancelados.toString(), VermelhoErro))
        add(CardMetrica("Estoque baixo", resumo.produtosEstoqueBaixo.toString(), AmareloDestaque))
        add(CardMetrica("Esgotados", resumo.produtosEsgotados.toString(), VermelhoErro))
        if (extras != null) {
            add(CardMetrica("Clientes", extras.totalClientes.toString(), RoxoNeon))
            add(CardMetrica("Produtos", extras.totalProdutos.toString(), RoxoNeon))
            add(CardMetrica("Aguardando pagamento", extras.pedidosPendentes.toString(), AmareloDestaque))
            add(CardMetrica("Em preparação", extras.pedidosEmPreparacao.toString(), RoxoNeon))
        }
    }
    val semDadosNoPeriodo = resumo.totalPedidos == 0L

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(Spacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = modifier.fillMaxWidth()
    ) {
        if (alertas.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    color = FundoCard,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Text("Alertas", style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
                        alertas.forEach { alerta ->
                            Text(alerta, style = MaterialTheme.typography.bodyMedium, color = TextoPrincipal)
                        }
                    }
                }
            }
        }
        if (semDadosNoPeriodo) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Nenhum dado disponível para o período selecionado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }
        }
        items(cards) { card -> CardMetricaItem(card) }
    }
}

/** "1 pedido aguarda" / "3 pedidos aguardam". */
private fun contagem(n: Long, singular: String, plural: String): String =
    if (n == 1L) "$n $singular" else "$n $plural"

private data class CardMetrica(val titulo: String, val valor: String, val cor: Color)

@Composable
private fun AtalhoCard(titulo: String, icone: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        color = FundoCard,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.width(96.dp).clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icone, null, tint = RoxoNeon)
            Spacer(Modifier.height(Spacing.xs))
            Text(titulo, style = MaterialTheme.typography.bodySmall, color = TextoPrincipal, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}


@Composable
private fun CardMetricaItem(card: CardMetrica) {
    Surface(
        color = FundoCard,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(card.titulo, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            Spacer(Modifier.height(Spacing.xs))
            Text(card.valor, style = MaterialTheme.typography.titleLarge, color = card.cor)
        }
    }
}
