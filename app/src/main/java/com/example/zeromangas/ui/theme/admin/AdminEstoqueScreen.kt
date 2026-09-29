package com.example.zeromangas.ui.theme.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.ui.components.EmptyState
import com.example.zeromangas.ui.components.LoadingState
import com.example.zeromangas.ui.theme.AmareloDestaque
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.VerdeSucesso
import com.example.zeromangas.ui.theme.VermelhoErro
import com.example.zeromangas.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

/**
 * Tela dedicada de Estoque (item 4 do pedido original) — mesma classificação
 * verde/amarelo/vermelho já usada na listagem de Produtos, mas aqui ordenada
 * pra mostrar primeiro quem precisa de atenção (esgotado, depois baixo).
 */
@Composable
fun AdminEstoqueScreen(adminViewModel: AdminViewModel, onVoltar: () -> Unit) {
    val produtos by adminViewModel.produtos.collectAsState()
    val carregando by adminViewModel.carregandoProdutos.collectAsState()
    val erro by adminViewModel.erroProdutos.collectAsState()
    val escopo = rememberCoroutineScope()
    var produtoAjustando by remember { mutableStateOf<Manga?>(null) }

    LaunchedEffect(Unit) { adminViewModel.carregarProdutos() }

    val produtosOrdenados = remember(produtos) {
        produtos.sortedBy { it.estoque }
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
            Text("Estoque", style = MaterialTheme.typography.headlineSmall, color = TextoPrincipal)
        }

        when {
            carregando && produtos.isEmpty() -> LoadingState(modifier = Modifier.weight(1f))
            erro != null && produtos.isEmpty() -> EmptyState(
                titulo = "Não foi possível carregar",
                subtitulo = erro,
                modifier = Modifier.weight(1f)
            )
            produtos.isEmpty() -> EmptyState(
                titulo = "Nenhum produto cadastrado",
                modifier = Modifier.weight(1f)
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(Spacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.weight(1f)
            ) {
                items(produtosOrdenados, key = { it.id }) { produto ->
                    ProdutoEstoqueItem(produto = produto, onClick = { produtoAjustando = produto })
                }
            }
        }
    }

    produtoAjustando?.let { produto ->
        DialogoAjustarEstoque(
            produto = produto,
            onDismiss = { produtoAjustando = null },
            onConfirmar = { novoEstoque ->
                escopo.launch {
                    adminViewModel.ajustarEstoque(produto.id, novoEstoque)
                    produtoAjustando = null
                }
            }
        )
    }
}

@Composable
private fun ProdutoEstoqueItem(produto: Manga, onClick: () -> Unit) {
    val cor = when {
        produto.estoque <= 0 -> VermelhoErro
        produto.estoque in 1..5 -> AmareloDestaque
        else -> VerdeSucesso
    }

    Surface(
        color = FundoCard,
        shape = MaterialTheme.shapes.large,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = produto.imagemUrl,
                contentDescription = produto.nome,
                modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small)
            )
            Spacer(Modifier.width(Spacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                Text(produto.nome, style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
                Text(produto.marca, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            }

            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(cor))
            Spacer(Modifier.width(Spacing.xs))
            Text(
                produto.estoque.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = cor,
                modifier = Modifier.width(36.dp)
            )
        }
    }
}

@Composable
private fun DialogoAjustarEstoque(produto: Manga, onDismiss: () -> Unit, onConfirmar: (Int) -> Unit) {
    var valor by remember { mutableStateOf(produto.estoque.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(produto.nome) },
        text = {
            OutlinedTextField(
                value = valor,
                onValueChange = { if (it.length <= 6) valor = it.filter(Char::isDigit) },
                label = { Text("Estoque") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirmar(valor.toIntOrNull() ?: 0) }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}