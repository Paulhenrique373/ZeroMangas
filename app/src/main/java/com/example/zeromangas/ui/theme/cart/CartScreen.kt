package com.example.zeromangas.ui.theme.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zeromangas.data.model.CartItem
import com.example.zeromangas.data.model.Cupom
import com.example.zeromangas.ui.components.EmptyState
import com.example.zeromangas.ui.components.PriceText
import com.example.zeromangas.ui.components.PrimaryButton
import com.example.zeromangas.ui.components.formatarPrecoBr
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.viewmodel.CartViewModel
import kotlinx.coroutines.delay

/** Interface de compra; toda regra continua no [CartViewModel]. */
@Composable
fun CartScreen(
    cartViewModel: CartViewModel,
    usuarioId: String,
    onVoltar: () -> Unit,
    onIrParaCheckout: () -> Unit,
    onExplorarClick: () -> Unit = {}
) {
    val itens by cartViewModel.itens.collectAsState()
    val cep by cartViewModel.cep.collectAsState()
    val cepErro by cartViewModel.cepErro.collectAsState()
    val frete by cartViewModel.frete.collectAsState()
    val calculandoFrete by cartViewModel.calculandoFrete.collectAsState()
    val cidadeUf by cartViewModel.cidadeUf.collectAsState()
    val avisoEstoque by cartViewModel.avisoEstoque.collectAsState()
    val cupomInput by cartViewModel.cupomInput.collectAsState()
    val cupomAplicado by cartViewModel.cupomAplicado.collectAsState()
    val cupomErro by cartViewModel.cupomErro.collectAsState()
    val validandoCupom by cartViewModel.validandoCupom.collectAsState()
    val desconto by cartViewModel.desconto.collectAsState()
    // O valor coletado por `by` não permite smart cast; esta referência local
    // mantém o mesmo dado para a composição do resumo abaixo.
    val cupomAtivo = cupomAplicado
    val subtotal = itens.sumOf { it.subtotal }
    val total = subtotal + (frete ?: 0.0) - desconto

    LaunchedEffect(avisoEstoque) {
        if (avisoEstoque != null) {
            delay(3_000)
            cartViewModel.limparAvisoEstoque()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(
                horizontal = Spacing.screenHorizontal,
                vertical = Spacing.lg
            )
        ) {
            Text("Seu carrinho", style = MaterialTheme.typography.headlineMedium, color = TextoPrincipal)
            Text(
                text = when (itens.sumOf { it.quantidade }) {
                    0 -> "Sua seleção está vazia"
                    1 -> "1 item reservado para você"
                    else -> "${itens.sumOf { it.quantidade }} itens reservados para você"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        avisoEstoque?.let { mensagem ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal)
            ) {
                Text(mensagem, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(Spacing.md))
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        if (itens.isEmpty()) {
            EmptyState(
                titulo = "Seu carrinho está vazio",
                subtitulo = "Quando encontrar sua próxima história, ela aparecerá aqui.",
                icone = Icons.Outlined.ShoppingCart,
                textoAcao = "Explorar mangás",
                onAcaoClick = onExplorarClick,
                modifier = Modifier.weight(1f)
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = Spacing.screenHorizontal,
                end = Spacing.screenHorizontal,
                bottom = Spacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
        ) {
            items(itens, key = { it.manga.id }) { item ->
                CartItemCard(
                    item = item,
                    onAumentar = { cartViewModel.aumentarQuantidade(item.manga) },
                    onDiminuir = { cartViewModel.diminuirQuantidade(item.manga) },
                    onRemover = { cartViewModel.removerItem(item.manga) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(Spacing.sm))
                SecaoFrete(
                    cep = cep, cepErro = cepErro, frete = frete, calculando = calculandoFrete,
                    cidadeUf = cidadeUf, onCepChange = cartViewModel::atualizarCep,
                    onCalcularFrete = cartViewModel::calcularFrete
                )
            }
            item {
                SecaoCupom(
                    cupomInput = cupomInput, cupomAplicado = cupomAplicado, cupomErro = cupomErro,
                    validando = validandoCupom, onCupomInputChange = cartViewModel::atualizarCupomInput,
                    onAplicarCupom = { cartViewModel.aplicarCupom(usuarioId) },
                    onRemoverCupom = cartViewModel::removerCupom
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = Spacing.subtleElevation,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md)) {
                LinhaResumo("Subtotal", subtotal)
                LinhaResumo("Frete", frete)
                LinhaResumo(
                    rotulo = if (cupomAtivo != null) "Desconto (${cupomAtivo.codigo})" else "Desconto",
                    valor = null,
                    textoAlternativo = if (desconto > 0) "− ${formatarPrecoBr(desconto)}" else "—",
                    emDestaque = desconto > 0
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total", style = MaterialTheme.typography.titleMedium, color = TextoPrincipal)
                        Text("Inclui os valores selecionados", style = MaterialTheme.typography.labelSmall, color = TextoSecundario)
                    }
                    Text(
                        formatarPrecoBr(total),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.md))
                PrimaryButton("Finalizar compra", onIrParaCheckout, Modifier.fillMaxWidth())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecaoFrete(
    cep: String, cepErro: String?, frete: Double?, calculando: Boolean, cidadeUf: String?,
    onCepChange: (String) -> Unit, onCalcularFrete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocalShipping, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Spacing.iconMedium))
                Spacer(modifier = Modifier.width(Spacing.sm))
                Column {
                    Text("Entrega", style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
                    Text("Calcule o frete pelo seu CEP", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = cep, onValueChange = onCepChange, placeholder = { Text("00000-000") },
                    singleLine = true, isError = cepErro != null, enabled = !calculando,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Button(
                    onClick = onCalcularFrete, enabled = !calculando,
                    modifier = Modifier.height(Spacing.textFieldMinHeight), shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    if (calculando) CircularProgressIndicator(
                        modifier = Modifier.size(Spacing.iconSmall), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    ) else Text("Calcular")
                }
            }
            when {
                cepErro != null -> Text(cepErro, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = Spacing.xs))
                frete != null -> Text(
                    listOfNotNull(cidadeUf?.let { "Entrega para $it" }, "Frete ${formatarPrecoBr(frete)}").joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Spacing.sm)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecaoCupom(
    cupomInput: String, cupomAplicado: Cupom?, cupomErro: String?, validando: Boolean,
    onCupomInputChange: (String) -> Unit, onAplicarCupom: () -> Unit, onRemoverCupom: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocalOffer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Spacing.iconMedium))
                Spacer(modifier = Modifier.width(Spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tem um cupom?", style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
                    Text("Adicione seu código para ganhar desconto.", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                }
                if (cupomAplicado != null) IconButton(onClick = onRemoverCupom) {
                    Icon(Icons.Default.Close, "Remover cupom")
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))
            if (cupomAplicado != null) {
                Text("Cupom ${cupomAplicado.codigo} aplicado", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = cupomInput, onValueChange = onCupomInputChange,
                        placeholder = { Text("Código do cupom") }, singleLine = true,
                        isError = cupomErro != null, enabled = !validando, modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Button(onClick = onAplicarCupom, enabled = !validando, modifier = Modifier.height(Spacing.textFieldMinHeight), shape = MaterialTheme.shapes.small) {
                        if (validando) CircularProgressIndicator(modifier = Modifier.size(Spacing.iconSmall), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) else Text("Aplicar")
                    }
                }
                cupomErro?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = Spacing.xs)) }
            }
        }
    }
}

@Composable
fun CartItemCard(item: CartItem, onAumentar: () -> Unit, onDiminuir: () -> Unit, onRemover: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.manga.imagemUrl, contentDescription = item.manga.nome, contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 76.dp, height = 104.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.manga.nome, style = MaterialTheme.typography.titleSmall, color = TextoPrincipal, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("Volume ${item.manga.volume}", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    }
                    IconButton(onClick = onRemover, modifier = Modifier.size(Spacing.touchTarget)) {
                        Icon(Icons.Default.DeleteOutline, "Remover ${item.manga.nome}", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Spacing.iconMedium))
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
                PriceText(
                    preco = item.manga.precoPromocional ?: item.manga.preco,
                    precoAntigo = item.manga.preco.takeIf { item.manga.emPromocao && item.manga.precoPromocional != null && item.manga.precoPromocional < it }
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    QuantidadeSelector(item.quantidade, item.quantidade < item.manga.estoque, onDiminuir, onAumentar)
                    Text(formatarPrecoBr(item.subtotal), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun QuantidadeSelector(quantidade: Int, podeAumentar: Boolean, onDiminuir: () -> Unit, onAumentar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = Spacing.xs, vertical = Spacing.xs)
    ) {
        QuantidadeBotao(Icons.Default.Remove, "Diminuir quantidade", true, onDiminuir)
        Text(quantidade.toString(), style = MaterialTheme.typography.labelLarge, color = TextoPrincipal, modifier = Modifier.padding(horizontal = Spacing.sm))
        QuantidadeBotao(Icons.Default.Add, "Aumentar quantidade", podeAumentar, onAumentar)
    }
}

@Composable
private fun QuantidadeBotao(icone: androidx.compose.ui.graphics.vector.ImageVector, descricao: String, habilitado: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(28.dp).clip(CircleShape).clickable(enabled = habilitado, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icone, descricao, tint = if (habilitado) MaterialTheme.colorScheme.primary else TextoSecundario.copy(alpha = .4f), modifier = Modifier.size(Spacing.iconSmall))
    }
}

@Composable
fun LinhaResumo(rotulo: String, valor: Double?, textoAlternativo: String? = null, emDestaque: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
        Text(
            textoAlternativo ?: (valor?.let(::formatarPrecoBr) ?: "—"),
            style = MaterialTheme.typography.bodyMedium,
            color = if (emDestaque) MaterialTheme.colorScheme.primary else TextoSecundario
        )
    }
}
