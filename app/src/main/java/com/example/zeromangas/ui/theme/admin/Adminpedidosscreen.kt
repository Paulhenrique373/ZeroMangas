package com.example.zeromangas.ui.theme.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zeromangas.repository.ItemPedidoAdminDto
import com.example.zeromangas.repository.PedidoAdminDto
import com.example.zeromangas.ui.components.EmptyState
import com.example.zeromangas.ui.components.LoadingState
import com.example.zeromangas.ui.components.formatarPrecoBr
import com.example.zeromangas.ui.theme.AmareloDestaque
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.RoxoNeonClaro
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.VerdeSucesso
import com.example.zeromangas.ui.theme.VermelhoErro
import com.example.zeromangas.viewmodel.AdminPedidosViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private data class OpcaoFiltro(val label: String, val status: String)

private val opcoesFiltro = listOf(
    OpcaoFiltro("Todos", ""),
    OpcaoFiltro("Pendente", "PENDENTE"),
    OpcaoFiltro("Pago", "PAGAMENTO_APROVADO"),
    OpcaoFiltro("Em preparação", "EM_PREPARACAO"),
    OpcaoFiltro("Enviado", "ENVIADO"),
    OpcaoFiltro("Entregue", "ENTREGUE"),
    OpcaoFiltro("Cancelado", "CANCELADO")
)

private fun rotuloStatus(status: String): String = when (status) {
    "PENDENTE" -> "Pendente"
    "PAGAMENTO_APROVADO" -> "Pagamento aprovado"
    "EM_PREPARACAO" -> "Em preparação"
    "ENVIADO" -> "Enviado"
    "ENTREGUE" -> "Entregue"
    "CANCELADO" -> "Cancelado"
    else -> status
}

private fun corStatus(status: String): Color = when (status) {
    "PENDENTE" -> TextoSecundario
    "PAGAMENTO_APROVADO" -> AmareloDestaque
    "EM_PREPARACAO" -> RoxoNeon
    "ENVIADO" -> RoxoNeonClaro
    "ENTREGUE" -> VerdeSucesso
    "CANCELADO" -> VermelhoErro
    else -> TextoSecundario
}

/** Próximo passo da sequência (o banco só aceita esse). Null = não avança mais. */
private fun proximoStatus(status: String): String? = when (status) {
    "PENDENTE" -> "PAGAMENTO_APROVADO"
    "PAGAMENTO_APROVADO" -> "EM_PREPARACAO"
    "EM_PREPARACAO" -> "ENVIADO"
    "ENVIADO" -> "ENTREGUE"
    else -> null
}

private fun textoBotaoAvancar(proximo: String): String = when (proximo) {
    "PAGAMENTO_APROVADO" -> "Confirmar pagamento"
    "EM_PREPARACAO" -> "Iniciar preparação"
    "ENVIADO" -> "Marcar como enviado"
    "ENTREGUE" -> "Marcar como entregue"
    else -> "Avançar"
}

private fun podeCancelar(status: String): Boolean =
    status in setOf("PENDENTE", "PAGAMENTO_APROVADO", "EM_PREPARACAO")

private fun formatarData(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
        val entrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val data = entrada.parse(iso.take(19)) ?: return "—"
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(data)
    } catch (e: Exception) {
        "—"
    }
}

/** "PIX" -> "Pix", "CARTAO_CREDITO" -> "Cartao credito". Só deixa o texto do banco legível. */
private fun formatarTextoBanco(valor: String?): String {
    if (valor.isNullOrBlank()) return "—"
    return valor.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}

private fun codigoCurto(id: String): String = "#" + id.take(8).uppercase()

/**
 * Tela de Pedidos do Painel Administrativo (item 5): lista com filtro por status,
 * detalhes em bottom sheet, avanço de status e cancelamento com devolução de estoque.
 */
@Composable
fun AdminPedidosScreen(viewModel: AdminPedidosViewModel, onVoltar: () -> Unit) {
    val pedidos by viewModel.pedidos.collectAsState()
    val carregando by viewModel.carregando.collectAsState()
    val erro by viewModel.erro.collectAsState()
    val filtro by viewModel.filtroStatus.collectAsState()
    val itens by viewModel.itens.collectAsState()
    val carregandoItens by viewModel.carregandoItens.collectAsState()
    val erroItens by viewModel.erroItens.collectAsState()

    // Guarda o pedido aberto como "foto": se o filtro esconder ele depois de uma ação,
    // o sheet continua aberto mostrando o status novo.
    var selecionado by remember { mutableStateOf<PedidoAdminDto?>(null) }

    LaunchedEffect(Unit) { viewModel.carregar() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.screenHorizontal),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoltar) {
                Icon(Icons.Default.ArrowBack, "Voltar", tint = TextoPrincipal)
            }
            Spacer(Modifier.width(Spacing.sm))
            Text("Pedidos", style = MaterialTheme.typography.headlineSmall, color = TextoPrincipal)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            opcoesFiltro.forEach { opcao ->
                FilterChip(
                    selected = opcao.status == filtro,
                    onClick = { viewModel.selecionarFiltro(opcao.status) },
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
            carregando && pedidos.isEmpty() -> LoadingState(modifier = Modifier.weight(1f))
            erro != null && pedidos.isEmpty() -> EmptyState(
                titulo = "Não foi possível carregar",
                subtitulo = erro,
                modifier = Modifier.weight(1f)
            )
            pedidos.isEmpty() -> EmptyState(
                titulo = "Nenhum pedido encontrado",
                modifier = Modifier.weight(1f)
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(Spacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.weight(1f)
            ) {
                items(pedidos, key = { it.idPedido }) { pedido ->
                    PedidoAdminItem(pedido = pedido, onClick = { selecionado = pedido })
                }
            }
        }
    }

    selecionado?.let { pedido ->
        DetalhePedidoSheet(
            pedido = pedido,
            itens = itens,
            carregandoItens = carregandoItens,
            erroItens = erroItens,
            onCarregarItens = { viewModel.carregarItens(pedido.idPedido) },
            onFechar = { selecionado = null },
            onAvancar = { novoStatus ->
                viewModel.avancarStatus(pedido.idPedido, novoStatus).onSuccess {
                    selecionado = selecionado?.copy(status = novoStatus)
                }
            },
            onCancelar = {
                viewModel.cancelar(pedido.idPedido).onSuccess {
                    selecionado = selecionado?.copy(status = "CANCELADO")
                }
            }
        )
    }
}

@Composable
private fun StatusPedidoChip(status: String) {
    val cor = corStatus(status)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Spacing.radiusPill))
            .background(cor.copy(alpha = 0.15f))
            .padding(horizontal = Spacing.sm, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(cor))
        Spacer(Modifier.width(4.dp))
        Text(
            text = rotuloStatus(status),
            style = MaterialTheme.typography.labelSmall,
            color = cor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PedidoAdminItem(pedido: PedidoAdminDto, onClick: () -> Unit) {
    Surface(
        color = FundoCard,
        shape = MaterialTheme.shapes.large,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pedido.nomeCliente?.takeIf { it.isNotBlank() } ?: "Cliente sem nome",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextoPrincipal,
                    modifier = Modifier.weight(1f)
                )
                StatusPedidoChip(pedido.status)
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "${codigoCurto(pedido.idPedido)} · ${formatarData(pedido.dataPedido)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )
            Spacer(Modifier.height(Spacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (pedido.qtdItens == 1L) "1 item" else "${pedido.qtdItens} itens",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatarPrecoBr(pedido.valorTotal),
                    style = MaterialTheme.typography.titleMedium,
                    color = RoxoNeonClaro
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetalhePedidoSheet(
    pedido: PedidoAdminDto,
    itens: List<ItemPedidoAdminDto>,
    carregandoItens: Boolean,
    erroItens: String?,
    onCarregarItens: () -> Unit,
    onFechar: () -> Unit,
    onAvancar: suspend (String) -> Result<Unit>,
    onCancelar: suspend () -> Result<Unit>
) {
    val escopo = rememberCoroutineScope()
    var processando by remember { mutableStateOf(false) }
    var erroAcao by remember { mutableStateOf<String?>(null) }
    var confirmandoCancelamento by remember { mutableStateOf(false) }
    var confirmandoAvanco by remember { mutableStateOf<String?>(null) }
    var mensagemSucesso by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pedido.idPedido) { onCarregarItens() }

    val proximo = proximoStatus(pedido.status)

    ModalBottomSheet(
        onDismissRequest = onFechar,
        containerColor = FundoCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(bottom = Spacing.lg)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Pedido ${codigoCurto(pedido.idPedido)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextoPrincipal,
                    modifier = Modifier.weight(1f)
                )
                StatusPedidoChip(pedido.status)
            }

            Text(
                text = pedido.nomeCliente?.takeIf { it.isNotBlank() } ?: "Cliente sem nome",
                style = MaterialTheme.typography.titleSmall,
                color = TextoPrincipal
            )
            if (!pedido.emailCliente.isNullOrBlank()) {
                Text(pedido.emailCliente, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            }
            Text(
                formatarData(pedido.dataPedido),
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )

            HorizontalDivider(color = TextoSecundario.copy(alpha = 0.2f))

            Text("Itens", style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
            when {
                carregandoItens -> Box(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = RoxoNeon) }
                erroItens != null -> Text(erroItens, style = MaterialTheme.typography.bodySmall, color = VermelhoErro)
                itens.isEmpty() -> Text("Sem itens.", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                else -> itens.forEach { item -> ItemPedidoLinha(item) }
            }

            HorizontalDivider(color = TextoSecundario.copy(alpha = 0.2f))

            LinhaInfo("Produtos", formatarPrecoBr(pedido.valorProdutos))
            LinhaInfo(
                "Frete" + (pedido.tipoFrete?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""),
                formatarPrecoBr(pedido.valorFrete)
            )
            if (pedido.valorDesconto > 0) {
                val cupom = pedido.cupomCodigo?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
                LinhaInfo("Desconto$cupom", "- " + formatarPrecoBr(pedido.valorDesconto), VerdeSucesso)
            }
            LinhaInfo("Total", formatarPrecoBr(pedido.valorTotal), RoxoNeonClaro, destaque = true)

            HorizontalDivider(color = TextoSecundario.copy(alpha = 0.2f))

            Text("Pagamento", style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
            if (pedido.metodoPagamento.isNullOrBlank() && pedido.statusPagamento.isNullOrBlank()) {
                Text(
                    "Nenhum pagamento registrado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            } else {
                LinhaInfo("Forma", formatarTextoBanco(pedido.metodoPagamento))
                LinhaInfo("Situação", formatarTextoBanco(pedido.statusPagamento))
            }

            Text("Entrega", style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
            Text(
                text = pedido.enderecoEntrega?.takeIf { it.isNotBlank() }
                    ?: pedido.cep?.takeIf { it.isNotBlank() }?.let { "CEP $it" }
                    ?: "Endereço não informado.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoPrincipal
            )

            if (erroAcao != null) {
                Text(erroAcao!!, style = MaterialTheme.typography.bodySmall, color = VermelhoErro)
            }
            if (mensagemSucesso != null) {
                Text(mensagemSucesso!!, style = MaterialTheme.typography.bodySmall, color = VerdeSucesso)
            }

            if (proximo != null) {
                Button(
                    onClick = { confirmandoAvanco = proximo },
                    enabled = !processando,
                    colors = ButtonDefaults.buttonColors(containerColor = RoxoNeon, contentColor = TextoPrincipal),
                    modifier = Modifier.fillMaxWidth().height(Spacing.buttonHeight)
                ) {
                    Text(textoBotaoAvancar(proximo))
                }
            }

            if (podeCancelar(pedido.status)) {
                OutlinedButton(
                    onClick = { confirmandoCancelamento = true },
                    enabled = !processando,
                    border = BorderStroke(1.dp, VermelhoErro),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VermelhoErro),
                    modifier = Modifier.fillMaxWidth().height(Spacing.buttonHeight)
                ) {
                    Text("Cancelar pedido")
                }
            }
        }
    }

    confirmandoAvanco?.let { novoStatus ->
        AlertDialog(
            onDismissRequest = { confirmandoAvanco = null },
            title = { Text("Alterar status?") },
            text = { Text("Deseja alterar o pedido para '${rotuloStatus(novoStatus)}'?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmandoAvanco = null
                    escopo.launch {
                        processando = true
                        erroAcao = null
                        mensagemSucesso = null
                        onAvancar(novoStatus)
                            .onSuccess { mensagemSucesso = "Status atualizado para '${rotuloStatus(novoStatus)}'." }
                            .onFailure { erroAcao = it.message }
                        processando = false
                    }
                }) { Text("Confirmar", color = RoxoNeonClaro) }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoAvanco = null }) { Text("Voltar") }
            }
        )
    }

    if (confirmandoCancelamento) {
        AlertDialog(
            onDismissRequest = { confirmandoCancelamento = false },
            title = { Text("Cancelar pedido?") },
            text = { Text("O estoque dos itens será devolvido. Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmandoCancelamento = false
                    escopo.launch {
                        processando = true
                        erroAcao = null
                        mensagemSucesso = null
                        onCancelar()
                            .onSuccess { mensagemSucesso = "Pedido cancelado e estoque devolvido." }
                            .onFailure { erroAcao = it.message }
                        processando = false
                    }
                }) { Text("Cancelar pedido", color = VermelhoErro) }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoCancelamento = false }) { Text("Voltar") }
            }
        )
    }
}

@Composable
private fun ItemPedidoLinha(item: ItemPedidoAdminDto) {
    val subtotal = if (item.subtotal > 0) item.subtotal else item.quantidade * item.precoUnitario
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = item.produtoImagemUrl,
            contentDescription = item.produtoNome,
            modifier = Modifier.size(44.dp).clip(MaterialTheme.shapes.small)
        )
        Spacer(Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.produtoNome ?: "Produto",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoPrincipal
            )
            Text(
                "${item.quantidade} x ${formatarPrecoBr(item.precoUnitario)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )
        }
        Text(formatarPrecoBr(subtotal), style = MaterialTheme.typography.bodyMedium, color = TextoPrincipal)
    }
}

@Composable
private fun LinhaInfo(
    rotulo: String,
    valor: String,
    corValor: Color = TextoPrincipal,
    destaque: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            rotulo,
            style = if (destaque) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = TextoSecundario,
            modifier = Modifier.weight(1f)
        )
        Text(
            valor,
            style = if (destaque) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = corValor
        )
    }
}