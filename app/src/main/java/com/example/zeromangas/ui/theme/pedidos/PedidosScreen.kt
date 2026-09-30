package com.example.zeromangas.ui.theme.pedidos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zeromangas.data.config.LojaConfig
import com.example.zeromangas.data.model.Order
import com.example.zeromangas.repository.OrderRepository
import com.example.zeromangas.ui.components.EmptyState
import com.example.zeromangas.ui.components.LoadingState
import com.example.zeromangas.ui.components.StatusBadge
import com.example.zeromangas.ui.components.formatarPrecoBr
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.RoxoNeonClaro
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.VermelhoErro
import com.example.zeromangas.viewmodel.CartViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tela de histórico de pedidos. Toda a lógica é a mesma de antes — status calculado
 * pelos estados persistidos do pedido, cancelamento via
 * [OrderRepository] — só o visual passou a usar o design system (FundoCard, EmptyState,
 * LoadingState, capa do mangá, badges de status coloridos).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosScreen(
    usuarioId: String,
    cartViewModel: CartViewModel,
    onIrParaCarrinho: () -> Unit,
    onVoltar: () -> Unit,
    onExplorarClick: () -> Unit = {}
) {
    val orderRepository = remember { OrderRepository() }
    val escopo = rememberCoroutineScope()

    var pedidos by remember { mutableStateOf<List<Order>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<String?>(null) }
    var idsCancelando by remember { mutableStateOf<Set<String>>(emptySet()) }
    var erroCancelamento by remember { mutableStateOf<String?>(null) }
    var idsRecomprando by remember { mutableStateOf<Set<String>>(emptySet()) }
    // Avisos de "indisponível" da recompra aparecem aqui mesmo (se nada entrou no
    // carrinho a pessoa continua nesta tela e precisa ver o motivo).
    val avisoRecompra by cartViewModel.avisoEstoque.collectAsState()

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

    fun comprarNovamente(pedido: Order) {
        idsRecomprando = idsRecomprando + pedido.id
        cartViewModel.comprarNovamente(pedido.itens) { adicionados ->
            idsRecomprando = idsRecomprando - pedido.id
            if (adicionados > 0) onIrParaCarrinho()
        }
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
            Column(modifier = Modifier.padding(start = Spacing.sm)) {
                Text("Meus pedidos", style = MaterialTheme.typography.headlineSmall, color = TextoPrincipal)
                if (pedidos.isNotEmpty()) {
                    Text(
                        text = "${pedidos.size} ${if (pedidos.size == 1) "pedido" else "pedidos"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                }
            }
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

        if (avisoRecompra != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .clip(RoundedCornerShape(Spacing.radiusSmall))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(Spacing.md)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = avisoRecompra ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { cartViewModel.limparAvisoEstoque() }) { Text("OK") }
                }
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
                    icone = Icons.Outlined.Inventory2,
                    textoAcao = "Explorar mangás",
                    onAcaoClick = onExplorarClick
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
                            cancelando = pedido.id in idsCancelando,
                            onCancelar = { cancelarPedido(pedido) },
                            recomprando = pedido.id in idsRecomprando,
                            onComprarNovamente = { comprarNovamente(pedido) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(Spacing.lg)) }
                }
            }
        }
    }
}

/**
 * Traduz o estado persistido no pedido para a interface. Não existe mais avanço
 * fictício pelo tempo: quando o backend passar a gravar PREPARANDO, ENVIADO ou
 * ENTREGUE, a timeline refletirá esses dados diretamente.
 */
private fun calcularStatusPedido(status: String): String {
    return when (status.uppercase()) {
        "CANCELADO" -> "Cancelado"
        "PREPARANDO", "PROCESSANDO", "EM_PREPARACAO" -> "Preparando"
        "ENVIADO", "EM_TRANSITO", "EM TRÂNSITO" -> "Enviado"
        "ENTREGUE", "CONCLUIDO", "CONCLUÍDO" -> "Entregue"
        else -> "Pedido confirmado"
    }
}

@Composable
fun PedidoCard(
    pedido: Order,
    cancelando: Boolean = false,
    onCancelar: () -> Unit = {},
    recomprando: Boolean = false,
    onComprarNovamente: (() -> Unit)? = null
) {
    val formatador = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")) }
    val statusAtual = remember(pedido.status) { calcularStatusPedido(pedido.status) }
    val podeCancelar = pedido.status.uppercase() in setOf("PAGAMENTO_APROVADO", "PROCESSANDO", "PREPARANDO", "EM_PREPARACAO")
    val primeiroItem = pedido.itens.firstOrNull()
    val itensRestantes = pedido.itens.size - 1

    var mostrarConfirmacao by remember { mutableStateOf(false) }
    val retiradaNaLoja = pedido.tipoFrete.equals(LojaConfig.TIPO_FRETE_RETIRADA, ignoreCase = true)
    val rotuloStatus = when {
        retiradaNaLoja && statusAtual == "Enviado" -> "Pronto para retirada"
        retiradaNaLoja && statusAtual == "Entregue" -> "Retirado"
        else -> statusAtual
    }
    // Pedido em andamento já abre com o acompanhamento visível.
    var detalhesExpandidos by remember { mutableStateOf(statusAtual != "Entregue" && statusAtual != "Cancelado") }

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
            StatusBadge(rotuloStatus)
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

        TextButton(
            onClick = { detalhesExpandidos = !detalhesExpandidos },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(if (detalhesExpandidos) "Ocultar detalhes" else "Ver detalhes")
        }

        if (detalhesExpandidos && statusAtual != "Cancelado") {
            Text("Acompanhamento", style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
            Spacer(modifier = Modifier.height(Spacing.xs))
            AcompanhamentoPedido(statusAtual = statusAtual, retiradaNaLoja = retiradaNaLoja)
            Spacer(modifier = Modifier.height(Spacing.sm))
            HorizontalDivider(color = TextoSecundario.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

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

        if (!podeCancelar && onComprarNovamente != null && pedido.itens.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.md))
            OutlinedButton(
                onClick = onComprarNovamente,
                enabled = !recomprando,
                shape = RoundedCornerShape(Spacing.radiusSmall),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (recomprando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Comprar novamente")
                }
            }
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

private data class EtapaTimeline(val titulo: String, val descricao: String, val icone: ImageVector)

/**
 * Linha do tempo vertical do pedido (confirmado -> preparando -> enviado/pronto
 * pra retirada -> entregue/retirado). Usa só o status real gravado no pedido —
 * não inventa datas nem previsões. Pedido de retirada na loja troca os textos
 * das duas últimas etapas e mostra o endereço da loja.
 */
@Composable
private fun AcompanhamentoPedido(statusAtual: String, retiradaNaLoja: Boolean = false) {
    val etapas = listOf(
        EtapaTimeline("Pedido confirmado", "Recebemos o seu pedido e o pagamento.", Icons.Default.Check),
        EtapaTimeline("Preparando", "Estamos separando os seus mangás.", Icons.Default.Inventory),
        if (retiradaNaLoja) {
            EtapaTimeline("Pronto para retirada", "Retire em ${LojaConfig.ENDERECO_LINHA_1}, ${LojaConfig.BAIRRO}.", Icons.Default.Storefront)
        } else {
            EtapaTimeline("Enviado", "Seu pedido está a caminho do endereço de entrega.", Icons.Default.LocalShipping)
        },
        EtapaTimeline(if (retiradaNaLoja) "Retirado" else "Entregue", if (retiradaNaLoja) "Pedido retirado na loja. Boa leitura!" else "Pedido entregue. Boa leitura!", Icons.Default.TaskAlt)
    )
    val indiceAtual = when (statusAtual) {
        "Preparando" -> 1
        "Enviado" -> 2
        "Entregue" -> 3
        else -> 0
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        etapas.forEachIndexed { indice, etapa ->
            val concluida = indice <= indiceAtual
            val atual = indice == indiceAtual
            val cor = if (concluida) RoxoNeonClaro else TextoSecundario.copy(alpha = 0.35f)

            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
                    Box(
                        modifier = Modifier
                            .size(if (atual) 28.dp else 22.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (concluida) cor else Color.Transparent)
                            .border(2.dp, cor, RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (concluida) {
                            Icon(etapa.icone, contentDescription = null, tint = Color.White, modifier = Modifier.size(if (atual) 16.dp else 12.dp))
                        }
                    }
                    if (indice < etapas.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .weight(1f)
                                .background(if (indice < indiceAtual) RoxoNeonClaro else TextoSecundario.copy(alpha = 0.25f))
                        )
                    }
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Column(modifier = Modifier.padding(bottom = if (indice < etapas.lastIndex) Spacing.md else 0.dp)) {
                    Text(
                        text = etapa.titulo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (concluida) TextoPrincipal else TextoSecundario,
                        fontWeight = if (atual) FontWeight.Bold else FontWeight.Normal
                    )
                    if (atual) {
                        Text(text = etapa.descricao, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    }
                }
            }
        }
    }
}