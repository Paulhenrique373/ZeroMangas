package com.example.zeromangas.ui.theme.checkout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.example.zeromangas.data.model.Endereco
import com.example.zeromangas.ui.components.PrimaryButton
import com.example.zeromangas.ui.components.formatarPrecoBr
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.cart.LinhaResumo
import com.example.zeromangas.ui.theme.cart.SecaoFrete
import com.example.zeromangas.viewmodel.CartViewModel
import com.example.zeromangas.viewmodel.CheckoutState

private enum class EtapaCheckout { ENTREGA, PAGAMENTO, RESUMO }
private data class MetodoPagamento(val nome: String, val icone: ImageVector)

private val metodosPagamento = listOf(
    MetodoPagamento("Cartão de Crédito", Icons.Default.CreditCard),
    MetodoPagamento("Pix", Icons.Default.QrCode),
    MetodoPagamento("Boleto", Icons.Default.ReceiptLong)
)

/** Checkout em etapas. Endereços, frete, cupom e confirmação continuam no CartViewModel. */
@Composable
fun CheckoutScreen(
    cartViewModel: CartViewModel,
    usuarioId: String,
    onVoltar: () -> Unit,
    onCompraFinalizada: (String) -> Unit
) {
    val itens by cartViewModel.itens.collectAsState()
    val cep by cartViewModel.cep.collectAsState()
    val cepErro by cartViewModel.cepErro.collectAsState()
    val frete by cartViewModel.frete.collectAsState()
    val calculandoFrete by cartViewModel.calculandoFrete.collectAsState()
    val cidadeUf by cartViewModel.cidadeUf.collectAsState()
    val numero by cartViewModel.numero.collectAsState()
    val complemento by cartViewModel.complemento.collectAsState()
    val desconto by cartViewModel.desconto.collectAsState()
    val checkoutState by cartViewModel.checkoutState.collectAsState()
    val enderecosSalvos by cartViewModel.enderecosSalvos.collectAsState()
    val carregandoEnderecos by cartViewModel.carregandoEnderecosSalvos.collectAsState()
    val enderecoSelecionadoId by cartViewModel.enderecoSelecionadoId.collectAsState()
    val subtotal = itens.sumOf { it.subtotal }
    val total = subtotal + (frete ?: 0.0) - desconto

    var etapa by remember { mutableStateOf(EtapaCheckout.ENTREGA) }
    var metodoSelecionado by remember { mutableStateOf<String?>(null) }
    var escolhendoEndereco by remember { mutableStateOf(true) }

    LaunchedEffect(usuarioId) { cartViewModel.carregarEnderecosSalvos(usuarioId) }
    LaunchedEffect(checkoutState) {
        if (checkoutState is CheckoutState.Sucesso) {
            val pedidoId = (checkoutState as CheckoutState.Sucesso).pedidoId
            cartViewModel.resetarCheckout()
            onCompraFinalizada(pedidoId)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                when (etapa) {
                    EtapaCheckout.ENTREGA -> onVoltar()
                    EtapaCheckout.PAGAMENTO -> etapa = EtapaCheckout.ENTREGA
                    EtapaCheckout.RESUMO -> etapa = EtapaCheckout.PAGAMENTO
                }
            }) { Icon(Icons.Default.ArrowBack, "Voltar", tint = TextoPrincipal) }
            Text("Checkout", style = MaterialTheme.typography.titleLarge, color = TextoPrincipal, modifier = Modifier.padding(start = Spacing.xs))
        }

        IndicadorDeEtapas(etapa)
        HorizontalDivider(modifier = Modifier.padding(top = Spacing.md), color = MaterialTheme.colorScheme.outlineVariant)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(Spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
        ) {
            when (etapa) {
                EtapaCheckout.ENTREGA -> item {
                    CabecalhoEtapa("1. Entrega", "Escolha onde deseja receber seu pedido")
                    if (carregandoEnderecos && enderecosSalvos.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(Spacing.xl), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (!escolhendoEndereco && enderecoSelecionadoId != null) {
                        val endereco = enderecosSalvos.firstOrNull { it.id == enderecoSelecionadoId }
                        if (endereco != null) CartaoEndereco(endereco, true, {})
                        TextButton(onClick = { escolhendoEndereco = true }) { Text("Alterar endereço") }
                    } else {
                        enderecosSalvos.forEach { endereco ->
                            CartaoEndereco(endereco, enderecoSelecionadoId == endereco.id) {
                                cartViewModel.selecionarEnderecoSalvo(endereco)
                                escolhendoEndereco = false
                            }
                            Spacer(Modifier.height(Spacing.sm))
                        }
                        OutlinedButton(
                            onClick = {
                                cartViewModel.selecionarNovoEndereco()
                                escolhendoEndereco = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(Spacing.iconMedium))
                            Spacer(Modifier.width(Spacing.xs))
                            Text("Adicionar endereço")
                        }
                    }

                    if (enderecoSelecionadoId == null) {
                        Spacer(Modifier.height(Spacing.sm))
                        SecaoFrete(cep, cepErro, frete, calculandoFrete, cidadeUf, cartViewModel::atualizarCep, cartViewModel::calcularFrete)
                        if (frete != null) {
                            Spacer(Modifier.height(Spacing.sm))
                            OutlinedTextField(numero, cartViewModel::atualizarNumero, Modifier.fillMaxWidth(), label = { Text("Número") }, singleLine = true, isError = numero.isBlank())
                            if (numero.isBlank()) Text("Informe o número para continuar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(Spacing.sm))
                            OutlinedTextField(complemento, cartViewModel::atualizarComplemento, Modifier.fillMaxWidth(), label = { Text("Complemento (opcional)") }, singleLine = true)
                        }
                    }
                }

                EtapaCheckout.PAGAMENTO -> item {
                    CabecalhoEtapa("2. Pagamento", "Selecione como deseja pagar")
                    metodosPagamento.forEach { metodo ->
                        CartaoMetodoPagamento(metodo, metodoSelecionado == metodo.nome) { metodoSelecionado = metodo.nome }
                        Spacer(Modifier.height(Spacing.sm))
                    }
                }

                EtapaCheckout.RESUMO -> {
                    item {
                        CabecalhoEtapa("3. Resumo", "Revise seu pedido antes de confirmar")
                        val endereco = enderecosSalvos.firstOrNull { it.id == enderecoSelecionadoId }
                        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(Spacing.md)) {
                                Text("Entrega", style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
                                Text(
                                    endereco?.let { "${it.logradouro}, ${it.numero} · ${it.cidade}/${it.uf}" } ?: "Endereço informado pelo CEP",
                                    style = MaterialTheme.typography.bodySmall, color = TextoSecundario
                                )
                                TextButton(onClick = { etapa = EtapaCheckout.ENTREGA }) { Text("Alterar") }
                            }
                        }
                        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Pagamento", style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
                                    Text(metodoSelecionado.orEmpty(), style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                                }
                                TextButton(onClick = { etapa = EtapaCheckout.PAGAMENTO }) { Text("Alterar") }
                            }
                        }
                    }
                    item {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(Spacing.md)) {
                                Text("Produtos", style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
                                Spacer(Modifier.height(Spacing.sm))
                                itens.forEach { item ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${item.quantidade}x ${item.manga.nome}", style = MaterialTheme.typography.bodyMedium, color = TextoSecundario, modifier = Modifier.weight(1f))
                                        Text(formatarPrecoBr(item.subtotal), style = MaterialTheme.typography.bodyMedium, color = TextoPrincipal)
                                    }
                                    Spacer(Modifier.height(Spacing.xs))
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = Spacing.sm))
                                LinhaResumo("Subtotal", subtotal)
                                LinhaResumo("Frete", frete)
                                LinhaResumo("Desconto", null, if (desconto > 0) "− ${formatarPrecoBr(desconto)}" else "—", desconto > 0)
                                Spacer(Modifier.height(Spacing.sm))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total", style = MaterialTheme.typography.titleMedium, color = TextoPrincipal)
                                    Text(formatarPrecoBr(total), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    if (checkoutState is CheckoutState.Erro) item {
                        Text((checkoutState as CheckoutState.Erro).mensagem, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = Spacing.subtleElevation, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md)) {
                when (etapa) {
                    EtapaCheckout.ENTREGA -> PrimaryButton(
                        "Continuar para pagamento", { etapa = EtapaCheckout.PAGAMENTO }, Modifier.fillMaxWidth(),
                        enabled = frete != null && numero.isNotBlank()
                    )
                    EtapaCheckout.PAGAMENTO -> PrimaryButton(
                        "Revisar pedido", { etapa = EtapaCheckout.RESUMO }, Modifier.fillMaxWidth(),
                        enabled = metodoSelecionado != null
                    )
                    EtapaCheckout.RESUMO -> PrimaryButton(
                        "Confirmar pedido", { metodoSelecionado?.let { cartViewModel.finalizarCompra(usuarioId, it) } }, Modifier.fillMaxWidth(),
                        enabled = metodoSelecionado != null && checkoutState !is CheckoutState.Carregando,
                        loading = checkoutState is CheckoutState.Carregando
                    )
                }
            }
        }
    }
}

@Composable
private fun CabecalhoEtapa(titulo: String, descricao: String) {
    Column(modifier = Modifier.padding(bottom = Spacing.sm)) {
        Text(titulo, style = MaterialTheme.typography.titleLarge, color = TextoPrincipal)
        Text(descricao, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
    }
}

@Composable
private fun CartaoEndereco(endereco: Endereco, selecionado: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selecionado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(endereco.nomeDestinatario.ifBlank { "Endereço" }, style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
                Text("${endereco.logradouro}, ${endereco.numero} · ${endereco.bairro}\n${endereco.cidade}/${endereco.uf}", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            }
            if (selecionado) Icon(Icons.Default.Check, "Selecionado", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun IndicadorDeEtapas(etapa: EtapaCheckout) {
    val atual = etapa.ordinal
    Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("Entrega", "Pagamento", "Resumo").forEachIndexed { indice, rotulo ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = if (indice <= atual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text("${indice + 1}", modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs), style = MaterialTheme.typography.labelMedium, color = if (indice <= atual) MaterialTheme.colorScheme.onPrimary else TextoSecundario)
                }
                Text(rotulo, style = MaterialTheme.typography.labelSmall, color = if (indice == atual) MaterialTheme.colorScheme.primary else TextoSecundario)
            }
        }
    }
}

@Composable
private fun CartaoMetodoPagamento(metodo: MetodoPagamento, selecionado: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selecionado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Icon(metodo.icone, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Spacing.md))
            Text(metodo.nome, style = MaterialTheme.typography.bodyLarge, color = TextoPrincipal, modifier = Modifier.weight(1f))
            if (selecionado) Icon(Icons.Default.Check, "Selecionado", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
