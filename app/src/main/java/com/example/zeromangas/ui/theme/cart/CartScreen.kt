package com.example.zeromangas.ui.theme.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zeromangas.data.model.CartItem
import com.example.zeromangas.data.model.Cupom
import com.example.zeromangas.ui.components.EmptyState
import com.example.zeromangas.ui.components.PriceText
import com.example.zeromangas.ui.components.PrimaryButton
import com.example.zeromangas.ui.components.formatarPrecoBr
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.RoxoNeonClaro
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.viewmodel.CartViewModel

/**
 * Tela do carrinho. A lógica (quantidade, frete via ViaCEP, cupom) é 100% a mesma do
 * [CartViewModel] já existente — só o visual muda, agora usando os componentes do
 * design system (PriceText, PrimaryButton, EmptyState). O checkout (endereço/pagamento)
 * agora é uma tela separada, a CheckoutScreen — "Finalizar Compra" só navega até ela.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    val subtotal = itens.sumOf { it.subtotal }
    val total = subtotal + (frete ?: 0.0) - desconto

    LaunchedEffect(avisoEstoque) {
        if (avisoEstoque != null) {
            delay(3000)
            cartViewModel.limparAvisoEstoque()
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
                text = "Meu Carrinho",
                style = MaterialTheme.typography.titleLarge,
                color = TextoPrincipal,
                modifier = Modifier.padding(start = Spacing.sm)
            )
        }

        if (avisoEstoque != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .clip(RoundedCornerShape(Spacing.radiusSmall))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(Spacing.md)
            ) {
                Text(
                    text = avisoEstoque ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        if (itens.isEmpty()) {
            EmptyState(
                titulo = "Seu carrinho está vazio",
                subtitulo = "Adicione mangás para vê-los aqui.",
                icone = Icons.Outlined.ShoppingCart,
                textoAcao = "Explorar catálogo",
                onAcaoClick = onExplorarClick
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
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
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    SecaoFrete(
                        cep = cep,
                        cepErro = cepErro,
                        frete = frete,
                        calculando = calculandoFrete,
                        cidadeUf = cidadeUf,
                        onCepChange = { cartViewModel.atualizarCep(it) },
                        onCalcularFrete = { cartViewModel.calcularFrete() }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    SecaoCupom(
                        cupomInput = cupomInput,
                        cupomAplicado = cupomAplicado,
                        cupomErro = cupomErro,
                        validando = validandoCupom,
                        onCupomInputChange = { cartViewModel.atualizarCupomInput(it) },
                        onAplicarCupom = { cartViewModel.aplicarCupom(usuarioId) },
                        onRemoverCupom = { cartViewModel.removerCupom() }
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total", style = MaterialTheme.typography.titleMedium, color = TextoPrincipal)
                    Text(
                        text = formatarPrecoBr(total),
                        style = MaterialTheme.typography.titleLarge,
                        color = RoxoNeonClaro
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                PrimaryButton(
                    text = "Finalizar Compra",
                    onClick = onIrParaCheckout,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecaoFrete(
    cep: String,
    cepErro: String?,
    frete: Double?,
    calculando: Boolean,
    cidadeUf: String?,
    onCepChange: (String) -> Unit,
    onCalcularFrete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Spacing.radiusMedium))
            .background(FundoCard)
            .padding(Spacing.md)
    ) {
        Text("Calcular frete", style = MaterialTheme.typography.titleSmall, color = RoxoNeonClaro)
        Spacer(modifier = Modifier.height(Spacing.sm))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = cep,
                onValueChange = onCepChange,
                placeholder = { Text("00000-000") },
                singleLine = true,
                isError = cepErro != null,
                enabled = !calculando,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Button(
                onClick = onCalcularFrete,
                enabled = !calculando,
                shape = RoundedCornerShape(Spacing.radiusSmall)
            ) {
                if (calculando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Calcular")
                }
            }
        }

        if (cepErro != null) {
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(cepErro, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        } else if (frete != null) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            if (cidadeUf != null) {
                Text(
                    text = "Entrega para: $cidadeUf",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = "Frete: ${formatarPrecoBr(frete)}",
                style = MaterialTheme.typography.bodyMedium,
                color = RoxoNeonClaro
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecaoCupom(
    cupomInput: String,
    cupomAplicado: Cupom?,
    cupomErro: String?,
    validando: Boolean,
    onCupomInputChange: (String) -> Unit,
    onAplicarCupom: () -> Unit,
    onRemoverCupom: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Spacing.radiusMedium))
            .background(FundoCard)
            .padding(Spacing.md)
    ) {
        Text("Cupom de desconto", style = MaterialTheme.typography.titleSmall, color = RoxoNeonClaro)
        Spacer(modifier = Modifier.height(Spacing.sm))

        if (cupomAplicado != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "\"${cupomAplicado.codigo}\" aplicado",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = RoxoNeonClaro
                )
                IconButton(onClick = onRemoverCupom, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remover cupom", tint = TextoPrincipal, modifier = Modifier.size(18.dp))
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = cupomInput,
                    onValueChange = onCupomInputChange,
                    placeholder = { Text("Código do cupom") },
                    singleLine = true,
                    isError = cupomErro != null,
                    enabled = !validando,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Button(
                    onClick = onAplicarCupom,
                    enabled = !validando,
                    shape = RoundedCornerShape(Spacing.radiusSmall)
                ) {
                    if (validando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Aplicar")
                    }
                }
            }

            if (cupomErro != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(cupomErro, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun CartItemCard(
    item: CartItem,
    onAumentar: () -> Unit,
    onDiminuir: () -> Unit,
    onRemover: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Spacing.radiusMedium))
            .background(FundoCard)
            .padding(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(Spacing.radiusSmall))
                .background(MaterialTheme.colorScheme.background)
        ) {
            AsyncImage(
                model = item.manga.imagemUrl,
                contentDescription = item.manga.nome,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.manga.nome,
                style = MaterialTheme.typography.titleSmall,
                color = TextoPrincipal,
                maxLines = 1
            )
            Text(
                text = "Vol. ${item.manga.volume} · ${formatarPrecoBr(item.manga.preco)} un.",
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(verticalAlignment = Alignment.CenterVertically) {
                QuantidadeBotao(icone = Icons.Default.Remove, contentDescription = "Diminuir", onClick = onDiminuir)
                Text(
                    text = "${item.quantidade}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoPrincipal,
                    modifier = Modifier.padding(horizontal = Spacing.sm)
                )
                QuantidadeBotao(icone = Icons.Default.Add, contentDescription = "Aumentar", onClick = onAumentar)
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            PriceText(preco = item.subtotal)
            IconButton(onClick = onRemover, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Remover", tint = TextoSecundario, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Botão circular pequeno de +/- usado no stepper de quantidade do item do carrinho. */
@Composable
private fun QuantidadeBotao(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icone, contentDescription = contentDescription, tint = TextoPrincipal, modifier = Modifier.size(14.dp))
    }
}

@Composable
fun LinhaResumo(rotulo: String, valor: Double?, textoAlternativo: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
        Text(
            text = textoAlternativo ?: (if (valor != null) formatarPrecoBr(valor) else "—"),
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario
        )
    }
}