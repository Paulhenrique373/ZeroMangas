package com.example.zeromangas.ui.theme.pedidos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.zeromangas.data.model.Order
import com.example.zeromangas.repository.OrderRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosScreen(
    usuarioId: String,
    onVoltar: () -> Unit
) {
    val orderRepository = remember { OrderRepository() }
    var pedidos by remember { mutableStateOf<List<Order>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<String?>(null) }

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
                text = "Meus Pedidos",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        when {
            carregando -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            erro != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(erro ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
            pedidos.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Você ainda não fez nenhum pedido.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pedidos, key = { it.id }) { pedido ->
                        PedidoCard(pedido = pedido)
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

/**
 * Calcula o status do pedido com base em quanto tempo passou desde a compra,
 * já que o app não tem um sistema de logística real por trás.
 *
 * < 1 dia: Processando | 1 a 3 dias: Enviado | mais de 3 dias: Entregue
 */
private fun calcularStatusPedido(dataPedido: Long): String {
    val diasDesdeACompra = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - dataPedido)
    return when {
        diasDesdeACompra < 1 -> "Processando"
        diasDesdeACompra in 1..3 -> "Enviado"
        else -> "Entregue"
    }
}

@Composable
private fun corDoStatus(status: String): Color {
    return when (status) {
        "Processando" -> MaterialTheme.colorScheme.tertiary
        "Enviado" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
fun PedidoCard(pedido: Order) {
    val formatador = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")) }
    val statusAtual = remember(pedido.data) { calcularStatusPedido(pedido.data) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pedido #${pedido.id.takeLast(6).uppercase()}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(corDoStatus(statusAtual))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(statusAtual, style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = formatador.format(Date(pedido.data)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        pedido.itens.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${item.quantidade}x ${item.manga.nome}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "R$ ${"%.2f".format(item.subtotal)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Total", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "R$ ${"%.2f".format(pedido.valorTotal)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}