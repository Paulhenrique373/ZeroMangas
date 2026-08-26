package com.example.zeromangas.ui.theme.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.zeromangas.data.model.Endereco
import com.example.zeromangas.ui.components.PrimaryButton
import com.example.zeromangas.ui.components.formatarPrecoBr
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.RoxoNeonClaro
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.cart.LinhaResumo
import com.example.zeromangas.ui.theme.cart.SecaoFrete
import com.example.zeromangas.viewmodel.CartViewModel
import com.example.zeromangas.viewmodel.CheckoutState

private enum class EtapaCheckout { ENDERECO, PAGAMENTO }

private data class MetodoPagamento(val nome: String, val icone: ImageVector)

private val metodosPagamento = listOf(
    MetodoPagamento("Cartão de Crédito", Icons.Default.CreditCard),
    MetodoPagamento("Pix", Icons.Default.QrCode),
    MetodoPagamento("Boleto", Icons.Default.ReceiptLong)
)

/**
 * Tela de checkout, organizada em duas etapas (Endereço → Pagamento), como pedido no
 * planejamento. Toda a lógica continua 100% no [CartViewModel] já existente — esta tela
 * só reorganiza visualmente o que antes eram dois AlertDialogs dentro da CartScreen.
 * A etapa de Endereço reaproveita o [SecaoFrete] já usado no carrinho (mesmo cálculo de
 * frete via ViaCEP), então nenhuma lógica de negócio nova foi criada.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val cupomAplicado by cartViewModel.cupomAplicado.collectAsState()
    val desconto by cartViewModel.desconto.collectAsState()
    val checkoutState by cartViewModel.checkoutState.collectAsState()
    val enderecosSalvos by cartViewModel.enderecosSalvos.collectAsState()
    val carregandoEnderecosSalvos by cartViewModel.carregandoEnderecosSalvos.collectAsState()
    val enderecoSelecionadoId by cartViewModel.enderecoSelecionadoId.collectAsState()

    val subtotal = itens.sumOf { it.subtotal }
    val total = subtotal + (frete ?: 0.0) - desconto

    var etapa by remember { mutableStateOf(EtapaCheckout.ENDERECO) }
    var metodoSelecionado by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        cartViewModel.carregarEnderecosSalvos(usuarioId)
    }

    LaunchedEffect(checkoutState) {
        val estado = checkoutState
        if (estado is CheckoutState.Sucesso) {
            val pedidoId = estado.pedidoId
            cartViewModel.resetarCheckout()
            onCompraFinalizada(pedidoId)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (etapa == EtapaCheckout.PAGAMENTO) etapa = EtapaCheckout.ENDERECO else onVoltar()
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextoPrincipal)
            }
            Text(
                text = "Finalizar Compra",
                style = MaterialTheme.typography.titleLarge,
                color = TextoPrincipal,
                modifier = Modifier.padding(start = Spacing.sm)
            )
        }

        IndicadorDeEtapas(etapaAtual = etapa)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.lg)
        ) {
            Spacer(modifier = Modifier.height(Spacing.md))

            when (etapa) {
                EtapaCheckout.ENDERECO -> {
                    if (carregandoEnderecosSalvos && enderecosSalvos.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(Spacing.md), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = RoxoNeon, modifier = Modifier.size(28.dp))
                        }
                    }

                    if (enderecosSalvos.isNotEmpty()) {
                        Text("Escolha um endereço", style = MaterialTheme.typography.titleSmall, color = RoxoNeonClaro)
                        Spacer(modifier = Modifier.height(Spacing.sm))

                        enderecosSalvos.forEach { endereco ->
                            CartaoEnderecoSalvo(
                                endereco = endereco,
                                selecionado = enderecoSelecionadoId == endereco.id,
                                onClick = { cartViewModel.selecionarEnderecoSalvo(endereco) }
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                        }

                        CartaoNovoEndereco(
                            selecionado = enderecoSelecionadoId == null,
                            onClick = { cartViewModel.selecionarNovoEndereco() }
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }

                    if (enderecoSelecionadoId == null) {
                        SecaoFrete(
                            cep = cep,
                            cepErro = cepErro,
                            frete = frete,
                            calculando = calculandoFrete,
                            cidadeUf = cidadeUf,
                            onCepChange = { cartViewModel.atualizarCep(it) },
                            onCalcularFrete = { cartViewModel.calcularFrete() }
                        )

                        if (frete != null) {
                            Spacer(modifier = Modifier.height(Spacing.md))
                            OutlinedTextField(
                                value = numero,
                                onValueChange = { cartViewModel.atualizarNumero(it) },
                                label = { Text("Número") },
                                singleLine = true,
                                isError = numero.isBlank(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (numero.isBlank()) {
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                Text(
                                    text = "Informe o número para continuar.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            OutlinedTextField(
                                value = complemento,
                                onValueChange = { cartViewModel.atualizarComplemento(it) },
                                label = { Text("Complemento (opcional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                EtapaCheckout.PAGAMENTO -> {
                    Text("Escolha a forma de pagamento", style = MaterialTheme.typography.titleSmall, color = RoxoNeonClaro)
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    metodosPagamento.forEach { metodo ->
                        CartaoMetodoPagamento(
                            metodo = metodo,
                            selecionado = metodoSelecionado == metodo.nome,
                            onClick = { metodoSelecionado = metodo.nome }
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }

                    Spacer(modifier = Modifier.height(Spacing.md))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Spacing.radiusMedium))
                            .background(FundoCard)
                            .padding(Spacing.md)
                    ) {
                        Text("Resumo do pedido", style = MaterialTheme.typography.titleSmall, color = RoxoNeonClaro)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        LinhaResumo(rotulo = "Itens", valor = null, textoAlternativo = "${itens.sumOf { it.quantidade }}")
                        LinhaResumo(rotulo = "Subtotal", valor = subtotal)
                        LinhaResumo(rotulo = "Frete", valor = frete)
                        if (cupomAplicado != null) {
                            LinhaResumo(
                                rotulo = "Desconto (${cupomAplicado?.codigo})",
                                valor = null,
                                textoAlternativo = "- ${formatarPrecoBr(desconto)}"
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        HorizontalDivider(color = TextoSecundario.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", style = MaterialTheme.typography.titleMedium, color = TextoPrincipal)
                            Text(
                                text = formatarPrecoBr(total),
                                style = MaterialTheme.typography.titleMedium,
                                color = RoxoNeonClaro
                            )
                        }
                    }

                    if (checkoutState is CheckoutState.Erro) {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = (checkoutState as CheckoutState.Erro).mensagem,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
            when (etapa) {
                EtapaCheckout.ENDERECO -> {
                    PrimaryButton(
                        text = "Continuar para pagamento",
                        onClick = { etapa = EtapaCheckout.PAGAMENTO },
                        enabled = frete != null && numero.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                EtapaCheckout.PAGAMENTO -> {
                    PrimaryButton(
                        text = "Confirmar Pedido",
                        onClick = {
                            metodoSelecionado?.let { cartViewModel.finalizarCompra(usuarioId, it) }
                        },
                        enabled = metodoSelecionado != null && checkoutState !is CheckoutState.Carregando,
                        loading = checkoutState is CheckoutState.Carregando,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/** Card selecionável de um endereço já salvo, na etapa de Endereço do checkout. */
@Composable
private fun CartaoEnderecoSalvo(
    endereco: Endereco,
    selecionado: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Spacing.radiusMedium))
            .background(if (selecionado) RoxoNeon.copy(alpha = 0.12f) else FundoCard)
            .clickable { onClick() }
            .padding(Spacing.md),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = if (selecionado) RoxoNeonClaro else TextoSecundario
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = endereco.nomeDestinatario.ifBlank { "Endereço" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selecionado) FontWeight.Medium else FontWeight.Normal,
                    color = if (selecionado) TextoPrincipal else TextoSecundario
                )
                if (endereco.padrao) {
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("· Padrão", style = MaterialTheme.typography.labelSmall, color = RoxoNeonClaro)
                }
            }
            Text(
                text = "${endereco.logradouro}, ${endereco.numero} - ${endereco.bairro}, ${endereco.cidade}/${endereco.uf}",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )
        }
        if (selecionado) {
            Icon(Icons.Default.Check, contentDescription = "Selecionado", tint = RoxoNeonClaro)
        }
    }
}

/** Card que troca pro formulário de CEP manual, pra digitar um endereço novo na hora. */
@Composable
private fun CartaoNovoEndereco(
    selecionado: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Spacing.radiusMedium))
            .background(if (selecionado) RoxoNeon.copy(alpha = 0.12f) else FundoCard)
            .clickable { onClick() }
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = if (selecionado) RoxoNeonClaro else TextoSecundario
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(
            text = "Usar um novo endereço",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selecionado) FontWeight.Medium else FontWeight.Normal,
            color = if (selecionado) TextoPrincipal else TextoSecundario,
            modifier = Modifier.weight(1f)
        )
        if (selecionado) {
            Icon(Icons.Default.Check, contentDescription = "Selecionado", tint = RoxoNeonClaro)
        }
    }
}

/** Indicador simples de progresso "1 Endereço — 2 Pagamento" no topo do checkout. */
@Composable
private fun IndicadorDeEtapas(etapaAtual: EtapaCheckout) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PassoEtapa(numero = 1, rotulo = "Endereço", ativo = true)
        HorizontalDivider(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.xs),
            color = if (etapaAtual == EtapaCheckout.PAGAMENTO) RoxoNeon else TextoSecundario.copy(alpha = 0.3f)
        )
        PassoEtapa(numero = 2, rotulo = "Pagamento", ativo = etapaAtual == EtapaCheckout.PAGAMENTO)
    }
}

@Composable
private fun PassoEtapa(numero: Int, rotulo: String, ativo: Boolean) {
    val cor = if (ativo) RoxoNeon else TextoSecundario
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(50))
                .background(if (ativo) RoxoNeon else FundoCard),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$numero",
                style = MaterialTheme.typography.labelSmall,
                color = if (ativo) androidx.compose.ui.graphics.Color.White else TextoSecundario
            )
        }
        Spacer(modifier = Modifier.width(Spacing.xs))
        Text(text = rotulo, style = MaterialTheme.typography.labelMedium, color = cor)
    }
}

/** Card selecionável de forma de pagamento (Cartão / Pix / Boleto). */
@Composable
private fun CartaoMetodoPagamento(
    metodo: MetodoPagamento,
    selecionado: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Spacing.radiusMedium))
            .background(if (selecionado) RoxoNeon.copy(alpha = 0.12f) else FundoCard)
            .clickable { onClick() }
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = metodo.icone,
            contentDescription = null,
            tint = if (selecionado) RoxoNeonClaro else TextoSecundario
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(
            text = metodo.nome,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selecionado) FontWeight.Medium else FontWeight.Normal,
            color = if (selecionado) TextoPrincipal else TextoSecundario,
            modifier = Modifier.weight(1f)
        )
        if (selecionado) {
            Icon(Icons.Default.Check, contentDescription = "Selecionado", tint = RoxoNeonClaro)
        }
    }
}