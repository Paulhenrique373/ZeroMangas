package com.example.zeromangas.ui.theme.pedidos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zeromangas.data.model.Order
import com.example.zeromangas.repository.OrderRepository
import com.example.zeromangas.ui.components.EmptyState
import com.example.zeromangas.ui.components.LoadingState
import com.example.zeromangas.ui.components.formatarPrecoBr
import com.example.zeromangas.ui.theme.AmareloDestaque
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.RoxoNeonClaro
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.VerdeSucesso
import com.example.zeromangas.ui.theme.VermelhoErro
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Tela de histórico de pedidos. Toda a lógica é a mesma de antes — status calculado
 * pelo tempo decorrido (Processando -> Enviado -> Entregue), cancelamento via
 * [OrderRepository] — só o visual passou a usar o design system (FundoCard, EmptyState,
 * LoadingState, capa do mangá, badges de status coloridos).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosScreen(
    usuarioId: String,
    onVoltar: () -> Unit
) {
    val orderRepository = remember { OrderRepository() }
    val escopo = rememberCoroutineScope()

    var pedidos by remember { mutableStateOf<List<Order>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<String?>(null) }
    var idsCancelando by remember { mutableStateOf<Set<String>>(emptySet()) }
    var erroCancelamento by remember { mutableStateOf<String?>(null) }

    // Relógio interno: atualiza a cada 5 segundos para que o status dos pedidos
    // (Processando -> Enviado -> Entregue) evolua visualmente sem precisar sair da tela.
    var agora by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            agora = System.currentTimeMillis()
        }
    }

    LaunchedEffect(usuarioId) {
        carregando = true
        erro = null
        val resultado = orderRepository.listarPedidosDoUsuario(usuarioId)
        resultado.onSuccess { lista ->
            pedidos = lista.sortedByDescending { it.data }
        }.onFailure {
            erro = "Não foi possível carregar seus pedidos."
        }
        carregando = false
    }

    fun cancelarPedido(pedido: Order) {
        idsCancelando = idsCancelando + pedido.id
        escopo.launch {
            val resultado = orderRepository.cancelarPedido(pedido.id)
            resultado.fold(
                onSuccess = {
                    pedidos = pedidos.map {
                        if (it.id == pedido.id) it.copy(status = "CANCELADO") else it
                    }
                },
                onFailure = {
                    erroCancelamento = "Não foi possível cancelar o pedido. Tente novamente."
                }
            )
            idsCancelando = idsCancelando - pedido.id
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoltar) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextoPrincipal)
            }
            Text(
                text = "Meus Pedidos",
                style = MaterialTheme.typography.titleLarge,
                color = TextoPrincipal,
                modifier = Modifier.padding(start = Spacing.sm)
            )
        }

        if (erroCancelamento != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .clip(RoundedCornerShape(Spacing.radiusSmall))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(Spacing.md)
            ) {
                Text(
                    text = erroCancelamento ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        when {
            carregando -> {
                LoadingState()
            }
            erro != null -> {
                EmptyState(
                    titulo = erro ?: "Não foi possível carregar seus pedidos.",
                    icone = Icons.Outlined.Inventory2
                )
            }
            pedidos.isEmpty() -> {
                EmptyState(
                    titulo = "Você ainda não fez nenhum pedido",
                    subtitulo = "Seus pedidos aparecerão aqui depois da primeira compra.",
                    icone = Icons.Outlined.Inventory2
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(pedidos, key = { it.id }) { pedido ->
                        PedidoCard(
                            pedido = pedido,
                            agora = agora,
                            cancelando = pedido.id in idsCancelando,
                            onCancelar = { cancelarPedido(pedido) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(Spacing.lg)) }
                }
            }
        }
    }
}

/**
 * Calcula o status do pedido com base em quanto tempo passou desde a compra,
 * a menos que o pedido já tenha sido cancelado manualmente pelo usuário.
 *
 * < 2 minutos: Processando | 2 a 5 minutos: Enviado | mais de 5 minutos: Entregue
 */
private fun calcularStatusPedido(pedido: Order, agora: Long): String {
    if (pedido.status == "CANCELADO") return "Cancelado"

    val minutosDesdeACompra = TimeUnit.MILLISECONDS.toMinutes(agora - pedido.data)
    return when {
        minutosDesdeACompra < 2 -> "Processando"
        minutosDesdeACompra in 2..4 -> "Enviado"
        else -> "Entregue"
    }
}

@Composable
private fun corDoStatus(status: String): Color {
    return when (status) {
        "Processando" -> AmareloDestaque
        "Enviado" -> RoxoNeonClaro
        "Cancelado" -> VermelhoErro
        else -> VerdeSucesso // Entregue
    }
}

/** Badge "● Status" com uma bolinha colorida, no padrão pedido no planejamento (🟡 Preparando). */
@Composable
private fun StatusBadge(status: String) {
    val cor = corDoStatus(status)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Spacing.radiusPill))
            .background(cor.copy(alpha = 0.15f))
            .padding(horizontal = Spacing.sm, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(cor)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = status, style = MaterialTheme.typography.labelSmall, color = cor, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PedidoCard(
    pedido: Order,
    agora: Long = System.currentTimeMillis(),
    cancelando: Boolean = false,
    onCancelar: () -> Unit = {}
) {
    val formatador = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")) }
    val statusAtual = remember(pedido.data, pedido.status, agora) { calcularStatusPedido(pedido, agora) }
    val podeCancelar = statusAtual == "Processando"
    val primeiroItem = pedido.itens.firstOrNull()
    val itensRestantes = pedido.itens.size - 1

    var mostrarConfirmacao by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Spacing.radiusMedium))
            .background(FundoCard)
            .padding(Spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pedido #${pedido.id.takeLast(6).uppercase()}",
                style = MaterialTheme.typography.titleSmall,
                color = RoxoNeonClaro
            )
            StatusBadge(statusAtual)
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = formatador.format(Date(pedido.data)),
            style = MaterialTheme.typography.labelSmall,
            color = TextoSecundario
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        if (primeiroItem != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(Spacing.radiusSmall))
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AsyncImage(
                        model = primeiroItem.manga.imagemUrl,
                        contentDescription = primeiroItem.manga.nome,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = primeiroItem.manga.nome,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoPrincipal,
                        maxLines = 1
                    )
                    Text(
                        text = if (itensRestantes > 0) {
                            "Vol. ${primeiroItem.manga.volume} · +$itensRestantes item(ns)"
                        } else {
                            "Vol. ${primeiroItem.manga.volume} · ${primeiroItem.quantidade}x"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoSecundario
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))
        HorizontalDivider(color = TextoSecundario.copy(alpha = 0.15f))
        Spacer(modifier = Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Total", style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
            Text(
                text = formatarPrecoBr(pedido.valorTotal),
                style = MaterialTheme.typography.titleSmall,
                color = RoxoNeonClaro
            )
        }

        if (podeCancelar) {
            Spacer(modifier = Modifier.height(Spacing.md))
            OutlinedButton(
                onClick = { mostrarConfirmacao = true },
                enabled = !cancelando,
                shape = RoundedCornerShape(Spacing.radiusSmall),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VermelhoErro),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (cancelando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = VermelhoErro
                    )
                } else {
                    Text("Cancelar pedido")
                }
            }
        }
    }

    if (mostrarConfirmacao) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacao = false },
            title = { Text("Cancelar pedido") },
            text = { Text("Tem certeza que deseja cancelar este pedido? Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacao = false
                    onCancelar()
                }) {
                    Text("Sim, cancelar", color = VermelhoErro)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacao = false }) {
                    Text("Voltar")
                }
            }
        )
    }
}