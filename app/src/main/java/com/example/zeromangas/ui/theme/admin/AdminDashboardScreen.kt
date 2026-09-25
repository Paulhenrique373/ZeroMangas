package com.example.zeromangas.ui.theme.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.zeromangas.repository.AdminRepository
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

private data class OpcaoPeriodo(val label: String, val dias: Int)

private val opcoesPeriodo = listOf(
    OpcaoPeriodo("7 dias", 7),
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
fun AdminDashboardScreen(adminRepository: AdminRepository, onVoltar: () -> Unit) {
    var periodoSelecionado by remember { mutableStateOf(opcoesPeriodo[1]) } // padrão: 30 dias
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<String?>(null) }
    var resumo by remember { mutableStateOf<DashboardResumoDto?>(null) }

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

        when {
            carregando -> LoadingState(modifier = Modifier.weight(1f))
            erro != null -> EmptyState(
                titulo = "Não foi possível carregar",
                subtitulo = erro,
                modifier = Modifier.weight(1f)
            )
            resumo != null -> DashboardConteudo(resumo!!, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DashboardConteudo(resumo: DashboardResumoDto, modifier: Modifier = Modifier) {
    val cards = listOf(
        CardMetrica("Faturamento", formatarPrecoBr(resumo.faturamentoTotal), VerdeSucesso),
        CardMetrica("Pedidos", resumo.totalPedidos.toString(), RoxoNeon),
        CardMetrica("Ticket médio", formatarPrecoBr(resumo.ticketMedio), RoxoNeon),
        CardMetrica("Novos clientes", resumo.novosClientes.toString(), RoxoNeon),
        CardMetrica("Cancelados", resumo.pedidosCancelados.toString(), VermelhoErro),
        CardMetrica("Estoque baixo", resumo.produtosEstoqueBaixo.toString(), AmareloDestaque),
        CardMetrica("Esgotados", resumo.produtosEsgotados.toString(), VermelhoErro)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(Spacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = modifier.fillMaxWidth()
    ) {
        items(cards) { card -> CardMetricaItem(card) }
    }
}

private data class CardMetrica(val titulo: String, val valor: String, val cor: Color)

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