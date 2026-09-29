package com.example.zeromangas.ui.theme.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.ui.components.EmptyState
import com.example.zeromangas.ui.components.LoadingState
import com.example.zeromangas.ui.components.formatarPrecoBr
import com.example.zeromangas.ui.theme.AmareloDestaque
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.VermelhoErro
import com.example.zeromangas.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

/**
 * Lista de produtos do Painel Administrativo (item 2 do pedido original).
 * Ao contrário do catálogo da loja, mostra TODOS os produtos, inclusive os
 * desativados (com o selo "Inativo" e o switch desligado).
 */
@Composable
fun AdminProdutosScreen(
    adminViewModel: AdminViewModel,
    onVoltar: () -> Unit,
    onNovoProdutoClick: () -> Unit,
    onEditarProdutoClick: (Manga) -> Unit
) {
    val produtos by adminViewModel.produtos.collectAsState()
    val carregando by adminViewModel.carregandoProdutos.collectAsState()
    val erro by adminViewModel.erroProdutos.collectAsState()
    val escopo = rememberCoroutineScope()

    LaunchedEffect(Unit) { adminViewModel.carregarProdutos() }

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
                Text("Produtos", style = MaterialTheme.typography.headlineSmall, color = TextoPrincipal)
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNovoProdutoClick, containerColor = RoxoNeon) {
                Icon(Icons.Default.Add, "Novo produto")
            }
        }
    ) { padding ->
        when {
            carregando && produtos.isEmpty() -> LoadingState(modifier = Modifier.fillMaxSize().padding(padding))
            erro != null && produtos.isEmpty() -> EmptyState(
                titulo = "Não foi possível carregar",
                subtitulo = erro,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            produtos.isEmpty() -> EmptyState(
                titulo = "Nenhum produto cadastrado",
                subtitulo = "Toque no + pra cadastrar o primeiro.",
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
                items(produtos, key = { it.id }) { produto ->
                    ProdutoAdminItem(
                        produto = produto,
                        onEditarClick = { onEditarProdutoClick(produto) },
                        onAlternarAtivo = { novoValor ->
                            escopo.launch { adminViewModel.alternarAtivo(produto.id, novoValor) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProdutoAdminItem(
    produto: Manga,
    onEditarClick: () -> Unit,
    onAlternarAtivo: (Boolean) -> Unit
) {
    Surface(color = FundoCard, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = produto.imagemUrl,
                contentDescription = produto.nome,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Spacer(Modifier.width(Spacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    produto.nome,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextoPrincipal,
                    textDecoration = if (!produto.ativo) TextDecoration.LineThrough else null
                )
                Text(
                    "${produto.marca} · ${produto.categoria}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatarPrecoBr(produto.preco),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoPrincipal
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        "Estoque: ${produto.estoque}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (produto.estoque <= 0) VermelhoErro
                        else if (produto.estoque in 1..5) AmareloDestaque
                        else TextoSecundario
                    )
                }
            }

            IconButton(onClick = onEditarClick) {
                Icon(Icons.Default.Edit, "Editar", tint = TextoSecundario)
            }
            Switch(checked = produto.ativo, onCheckedChange = onAlternarAtivo)
        }
    }
}