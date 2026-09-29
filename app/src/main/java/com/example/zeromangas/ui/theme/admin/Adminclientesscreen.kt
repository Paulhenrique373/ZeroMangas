package com.example.zeromangas.ui.theme.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zeromangas.repository.ClienteAdminDto
import com.example.zeromangas.ui.components.EmptyState
import com.example.zeromangas.ui.components.LoadingState
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.viewmodel.AdminClientesViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private fun formatarDataCliente(iso: String?, comHora: Boolean = false): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
        val entrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val data = entrada.parse(iso.take(19)) ?: return "—"
        val padrao = if (comHora) "dd/MM/yyyy HH:mm" else "dd/MM/yyyy"
        SimpleDateFormat(padrao, Locale("pt", "BR")).format(data)
    } catch (e: Exception) {
        "—"
    }
}

/**
 * Clientes do Painel Administrativo: consulta somente-leitura de dados básicos.
 * A RPC nunca devolve senha, CPF nem telefone.
 */
@Composable
fun AdminClientesScreen(viewModel: AdminClientesViewModel, onVoltar: () -> Unit) {
    val clientes by viewModel.clientes.collectAsState()
    val carregando by viewModel.carregando.collectAsState()
    val erro by viewModel.erro.collectAsState()

    var busca by remember { mutableStateOf("") }
    var selecionado by remember { mutableStateOf<ClienteAdminDto?>(null) }

    // Carga inicial e busca com pequena espera (não consulta a cada letra digitada).
    LaunchedEffect(busca) {
        delay(400)
        viewModel.carregar(busca)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.screenHorizontal),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoltar) {
                Icon(Icons.Default.ArrowBack, "Voltar", tint = TextoPrincipal)
            }
            Spacer(Modifier.width(Spacing.sm))
            Text("Clientes", style = MaterialTheme.typography.headlineSmall, color = TextoPrincipal)
        }

        OutlinedTextField(
            value = busca,
            onValueChange = { busca = it },
            placeholder = { Text("Buscar por nome ou e-mail") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal)
        )

        Spacer(Modifier.height(Spacing.md))

        when {
            carregando && clientes.isEmpty() -> LoadingState(modifier = Modifier.weight(1f))
            erro != null && clientes.isEmpty() -> EmptyState(
                titulo = "Não foi possível carregar",
                subtitulo = erro,
                textoAcao = "Tentar novamente",
                onAcaoClick = { viewModel.carregar(busca) },
                modifier = Modifier.weight(1f)
            )
            clientes.isEmpty() -> EmptyState(
                titulo = "Nenhum cliente encontrado.",
                modifier = Modifier.weight(1f)
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(Spacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.weight(1f)
            ) {
                items(clientes, key = { it.idUsuario }) { cliente ->
                    ClienteItem(cliente = cliente, onClick = { selecionado = cliente })
                }
            }
        }
    }

    selecionado?.let { cliente ->
        DetalheClienteSheet(cliente = cliente, onFechar = { selecionado = null })
    }
}

@Composable
private fun ClienteItem(cliente: ClienteAdminDto, onClick: () -> Unit) {
    Surface(
        color = FundoCard,
        shape = MaterialTheme.shapes.large,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = cliente.nome?.takeIf { it.isNotBlank() } ?: "Sem nome",
                style = MaterialTheme.typography.titleSmall,
                color = TextoPrincipal
            )
            if (!cliente.email.isNullOrBlank()) {
                Text(cliente.email, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = (if (cliente.qtdPedidos == 1L) "1 pedido" else "${cliente.qtdPedidos} pedidos") +
                        " · cadastro em ${formatarDataCliente(cliente.dataCadastro)}",
                style = MaterialTheme.typography.bodySmall,
                color = RoxoNeon
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetalheClienteSheet(cliente: ClienteAdminDto, onFechar: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onFechar, containerColor = FundoCard) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(bottom = Spacing.lg)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = cliente.nome?.takeIf { it.isNotBlank() } ?: "Sem nome",
                style = MaterialTheme.typography.titleLarge,
                color = TextoPrincipal
            )
            HorizontalDivider(color = TextoSecundario.copy(alpha = 0.2f))
            LinhaClienteInfo("E-mail", cliente.email?.takeIf { it.isNotBlank() } ?: "—")
            LinhaClienteInfo("Cadastro", formatarDataCliente(cliente.dataCadastro, comHora = true))
            LinhaClienteInfo("Status", cliente.status?.takeIf { it.isNotBlank() } ?: "—")
            LinhaClienteInfo("Pedidos", cliente.qtdPedidos.toString())
            LinhaClienteInfo(
                "Último pedido",
                if (cliente.ultimoPedido.isNullOrBlank()) "Nenhum pedido ainda"
                else formatarDataCliente(cliente.ultimoPedido, comHora = true)
            )
        }
    }
}

@Composable
private fun LinhaClienteInfo(rotulo: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            rotulo,
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario,
            modifier = Modifier.weight(1f)
        )
        Text(valor, style = MaterialTheme.typography.bodyMedium, color = TextoPrincipal)
    }
}