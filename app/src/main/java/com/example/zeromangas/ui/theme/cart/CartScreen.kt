package com.example.zeromangas.ui.theme.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.zeromangas.data.model.CartItem
import com.example.zeromangas.viewmodel.CartViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartViewModel: CartViewModel,
    onVoltar: () -> Unit,
    onFinalizarCompra: () -> Unit
) {
    val itens by cartViewModel.itens.collectAsState()
    val total = itens.sumOf { it.subtotal }

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
                    text = "Seu carrinho está vazio",
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
            }

            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
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

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onFinalizarCompra,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Finalizar Compra")
                }
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
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text("📖")
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