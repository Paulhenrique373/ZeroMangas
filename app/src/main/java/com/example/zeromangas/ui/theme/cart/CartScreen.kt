package com.example.zeromangas.ui.theme.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zeromangas.data.model.CartItem
import com.example.zeromangas.viewmodel.CartViewModel
import com.example.zeromangas.viewmodel.CheckoutState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartViewModel: CartViewModel,
    usuarioId: String,
    onVoltar: () -> Unit,
    onFinalizarCompra: () -> Unit
) {
    val itens by cartViewModel.itens.collectAsState()
    val cep by cartViewModel.cep.collectAsState()
    val cepErro by cartViewModel.cepErro.collectAsState()
    val frete by cartViewModel.frete.collectAsState()
    val checkoutState by cartViewModel.checkoutState.collectAsState()

    val subtotal = itens.sumOf { it.subtotal }
    val total = subtotal + (frete ?: 0.0)

    var mostrarResumo by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoltar) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
            }
            Text(
                text = "Meu Carrinho",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (itens.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Seu carrinho está vazio.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Spacer(modifier = Modifier.height(4.dp))
                    SecaoFrete(
                        cep = cep,
                        cepErro = cepErro,
                        frete = frete,
                        onCepChange = { cartViewModel.atualizarCep(it) },
                        onCalcularFrete = { cartViewModel.calcularFrete() }
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                LinhaResumo(rotulo = "Subtotal", valor = subtotal)
                LinhaResumo(rotulo = "Frete", valor = frete)
                Spacer(modifier = Modifier.height(4.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "R$ ${"%.2f".format(total)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (checkoutState is CheckoutState.Erro) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = (checkoutState as CheckoutState.Erro).mensagem,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { mostrarResumo = true },
                    enabled = checkoutState !is CheckoutState.Carregando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (checkoutState is CheckoutState.Carregando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Finalizar Compra")
                    }
                }
            }
        }
    }

    if (mostrarResumo) {
        AlertDialog(
            onDismissRequest = { mostrarResumo = false },
            title = { Text("Resumo da compra") },
            text = {
                Column {
                    LinhaResumo(rotulo = "Itens", valor = null, textoAlternativo = "${itens.sumOf { it.quantidade }}")
                    LinhaResumo(rotulo = "Subtotal", valor = subtotal)
                    LinhaResumo(rotulo = "Frete", valor = frete)
                    if (cep.isNotBlank()) {
                        LinhaResumo(rotulo = "CEP", valor = null, textoAlternativo = cep)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "R$ ${"%.2f".format(total)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    mostrarResumo = false
                    cartViewModel.finalizarCompra(usuarioId)
                }) {
                    Text("Confirmar compra")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarResumo = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (checkoutState is CheckoutState.Sucesso) {
        val pedidoId = (checkoutState as CheckoutState.Sucesso).pedidoId
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                TextButton(onClick = {
                    cartViewModel.resetarCheckout()
                    onFinalizarCompra()
                }) {
                    Text("Voltar para o início")
                }
            },
            title = { Text("Compra realizada com sucesso! 🎉") },
            text = { Text("Pedido nº $pedidoId") }
        )
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
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = textoAlternativo ?: (if (valor != null) "R$ ${"%.2f".format(valor)}" else "—"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecaoFrete(
    cep: String,
    cepErro: String?,
    frete: Double?,
    onCepChange: (String) -> Unit,
    onCalcularFrete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text("Calcular frete", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = cep,
                onValueChange = onCepChange,
                placeholder = { Text("00000-000") },
                singleLine = true,
                isError = cepErro != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Button(onClick = onCalcularFrete) {
                Text("Calcular")
            }
        }

        if (cepErro != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(cepErro, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        } else if (frete != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Frete: R$ ${"%.2f".format(frete)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
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
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.background)
        ) {
            AsyncImage(
                model = item.manga.imagemUrl,
                contentDescription = item.manga.nome,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.manga.nome,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Text(
                text = "R$ ${"%.2f".format(item.manga.preco)} un.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDiminuir, modifier = Modifier.size(28.dp)) {
                    Text(text = "−", style = MaterialTheme.typography.titleMedium)
                }
                Text(text = "${item.quantidade}", modifier = Modifier.padding(horizontal = 8.dp))
                IconButton(onClick = onAumentar, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Aumentar", modifier = Modifier.size(16.dp))
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "R$ ${"%.2f".format(item.subtotal)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onRemover) {
                Icon(Icons.Default.Delete, contentDescription = "Remover", modifier = Modifier.size(18.dp))
            }
        }
    }
}