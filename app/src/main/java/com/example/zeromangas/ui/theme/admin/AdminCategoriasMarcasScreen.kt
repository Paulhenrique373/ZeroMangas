package com.example.zeromangas.ui.theme.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.zeromangas.ui.components.EmptyState
import com.example.zeromangas.ui.components.LoadingState
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.VermelhoErro
import kotlinx.coroutines.launch

/** Um item genérico de nome (categoria ou marca/editora) — as duas telas usam a mesma UI. */
data class ItemNomeado(val id: String, val nome: String)

/**
 * Tela genérica de Categorias/Editoras (item 3 do pedido original) — visualizar e
 * editar/cadastrar, sem exclusão (evita quebrar produtos que já usam a categoria/marca).
 * Usada tanto pra "Categorias" quanto pra "Editoras", só trocando os parâmetros.
 */
@Composable
fun AdminCategoriasMarcasScreen(
    titulo: String,
    itens: List<ItemNomeado>,
    carregando: Boolean,
    onVoltar: () -> Unit,
    onRecarregar: () -> Unit,
    onSalvar: suspend (id: String?, nome: String) -> Result<Unit>
) {
    val escopo = rememberCoroutineScope()
    var itemEmEdicao by remember { mutableStateOf<ItemNomeado?>(null) }
    var criandoNovo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { onRecarregar() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.screenHorizontal),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onVoltar) {
                    Icon(Icons.Default.ArrowBack, "Voltar", tint = TextoPrincipal)
                }
                Spacer(Modifier.width(Spacing.sm))
                Text(titulo, style = MaterialTheme.typography.headlineSmall, color = TextoPrincipal)
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { criandoNovo = true }, containerColor = com.example.zeromangas.ui.theme.RoxoNeon) {
                Icon(Icons.Default.Add, "Adicionar")
            }
        }
    ) { padding ->
        when {
            carregando && itens.isEmpty() -> LoadingState(modifier = Modifier.fillMaxSize().padding(padding))
            itens.isEmpty() -> EmptyState(
                titulo = "Nada cadastrado ainda",
                subtitulo = "Toque no + pra adicionar.",
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = Spacing.screenHorizontal,
                    end = Spacing.screenHorizontal,
                    top = padding.calculateTopPadding(),
                    bottom = Spacing.xxl
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxSize()
            ) {
                items(itens, key = { it.id }) { item ->
                    Surface(color = FundoCard, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.nome, color = TextoPrincipal, modifier = Modifier.weight(1f))
                            IconButton(onClick = { itemEmEdicao = item }) {
                                Icon(Icons.Default.Edit, "Editar", tint = TextoPrincipal)
                            }
                        }
                    }
                }
            }
        }
    }

    if (criandoNovo || itemEmEdicao != null) {
        DialogoNomeSimples(
            tituloDialogo = if (itemEmEdicao == null) "Novo em \"$titulo\"" else "Editar",
            nomeInicial = itemEmEdicao?.nome ?: "",
            onDismiss = { criandoNovo = false; itemEmEdicao = null },
            onConfirmar = { nome ->
                escopo.launch {
                    onSalvar(itemEmEdicao?.id, nome)
                    criandoNovo = false
                    itemEmEdicao = null
                }
            }
        )
    }
}

@Composable
private fun DialogoNomeSimples(
    tituloDialogo: String,
    nomeInicial: String,
    onDismiss: () -> Unit,
    onConfirmar: (String) -> Unit
) {
    var nome by remember { mutableStateOf(nomeInicial) }
    var erro by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tituloDialogo) },
        text = {
            Column {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it; erro = null },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (erro != null) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(erro!!, color = VermelhoErro, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nome.isBlank()) erro = "O nome não pode ficar vazio" else onConfirmar(nome.trim())
            }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}