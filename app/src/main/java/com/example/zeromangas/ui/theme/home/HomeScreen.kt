package com.example.zeromangas.ui.theme.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.ui.components.CategoryChip
import com.example.zeromangas.ui.components.EmptyState
import com.example.zeromangas.ui.components.LoadingState
import com.example.zeromangas.ui.components.MangaCardFavoritavel
import com.example.zeromangas.ui.components.PrimaryButton
import com.example.zeromangas.ui.components.SecondaryButton
import com.example.zeromangas.ui.components.SectionHeader
import com.example.zeromangas.ui.theme.BordaSutil
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.viewmodel.CartViewModel
import com.example.zeromangas.viewmodel.FavoritoViewModel
import com.example.zeromangas.viewmodel.HomeViewModel
import com.example.zeromangas.viewmodel.NotificacaoViewModel
import com.example.zeromangas.viewmodel.TipoOrdenacao

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    cartViewModel: CartViewModel,
    favoritoViewModel: FavoritoViewModel,
    notificacaoViewModel: NotificacaoViewModel,
    usuarioId: String,
    onMangaClick: (Manga) -> Unit = {},
    onCarrinhoClick: () -> Unit = {},
    onPedidosClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    onFavoritosClick: () -> Unit = {},
    onBuscaClick: () -> Unit = {},
    onNotificacoesClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val mangasEmDestaque by homeViewModel.mangasEmDestaque.collectAsState()
    val mangasLancamentos by homeViewModel.mangasLancamentos.collectAsState()
    val mangasRecomendados by homeViewModel.mangasRecomendados.collectAsState()
    val categorias by homeViewModel.categorias.collectAsState()
    val carregando by homeViewModel.carregando.collectAsState()
    val erro by homeViewModel.erro.collectAsState()
    val categoriaSelecionada by homeViewModel.categoriaSelecionada.collectAsState()
    val itensCarrinho by cartViewModel.itens.collectAsState()
    val quantidadeNoCarrinho = itensCarrinho.sumOf { it.quantidade }
    val favoritosIds by favoritoViewModel.favoritosIds.collectAsState()
    val quantidadeNotificacoesNaoLidas by notificacaoViewModel.quantidadeNaoLidas.collectAsState()

    var mostrarConfirmacaoLogout by remember { mutableStateOf(false) }

    LaunchedEffect(usuarioId) {
        favoritoViewModel.carregarFavoritos(usuarioId)
        notificacaoViewModel.iniciar(usuarioId)
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ---- Topo: saudação + ícones de navegação ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.md, end = Spacing.sm, top = Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Olá! 👋",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "O que você quer ler hoje?",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                BadgedBox(
                    badge = {
                        if (quantidadeNotificacoesNaoLidas > 0) {
                            Badge { Text("$quantidadeNotificacoesNaoLidas") }
                        }
                    }
                ) {
                    IconButton(onClick = onNotificacoesClick) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notificações",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = onFavoritosClick) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Meus Favoritos",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                BadgedBox(
                    badge = {
                        if (quantidadeNoCarrinho > 0) {
                            Badge { Text("$quantidadeNoCarrinho") }
                        }
                    }
                ) {
                    IconButton(onClick = onCarrinhoClick) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Carrinho",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = onPedidosClick) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "Meus Pedidos",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onPerfilClick) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Meu Perfil",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = { mostrarConfirmacaoLogout = true }) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Sair da conta",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // ---- Busca ----
        // ETAPA 2 (Home): antes esse campo filtrava a própria Home (duplicando a
        // tela de Busca). Agora ele é só um atalho visual: ao tocar, ele NÃO edita
        // texto aqui — leva direto para a tela de Busca (que já tem toda a lógica
        // de filtros/ordenação), igual ao padrão de apps de loja (Amazon, Play Store etc).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .height(52.dp)
                .clip(RoundedCornerShape(Spacing.radiusSmall))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, BordaSutil, RoundedCornerShape(Spacing.radiusSmall))
                .clickable { onBuscaClick() }
                .padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = "Buscar por nome, marca ou volume...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // ---- Categorias ----
        // Tocar numa categoria já seleciona ela no HomeViewModel (compartilhado com
        // a tela de Busca) e leva para lá, já mostrando o resultado filtrado.
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            items(categorias, key = { it }) { categoria ->
                CategoryChip(
                    texto = categoria,
                    selecionado = categoriaSelecionada == categoria,
                    onClick = {
                        homeViewModel.selecionarCategoria(categoria)
                        onBuscaClick()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // ---- Conteúdo principal ----
        when {
            carregando && mangasEmDestaque.isEmpty() && mangasLancamentos.isEmpty() -> {
                LoadingState(modifier = Modifier.weight(1f))
            }

            erro != null && mangasEmDestaque.isEmpty() && mangasLancamentos.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = erro ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    PrimaryButton(
                        text = "Tentar novamente",
                        onClick = { homeViewModel.carregarDados() }
                    )
                }
            }

            mangasEmDestaque.isEmpty() && mangasLancamentos.isEmpty() -> {
                EmptyState(
                    titulo = "Catálogo vazio",
                    subtitulo = "Ainda não há mangás cadastrados no momento.",
                    modifier = Modifier.weight(1f)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = Spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // Banner de destaque: usa o primeiro mangá marcado como destaque
                    val destaque = mangasEmDestaque.firstOrNull()
                    if (destaque != null) {
                        item {
                            BannerDestaque(
                                manga = destaque,
                                onClick = { onMangaClick(destaque) }
                            )
                        }
                    }

                    // Mais vendidos: usa os mangás marcados como destaque
                    // (não há contagem real de vendas hoje)
                    if (mangasEmDestaque.isNotEmpty()) {
                        item {
                            SectionHeader(titulo = "🔥 Mais vendidos", onVerTodosClick = onBuscaClick)
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = Spacing.md),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                items(mangasEmDestaque, key = { "destaque_${it.id}" }) { manga ->
                                    MangaCardFavoritavel(
                                        manga = manga,
                                        isFavorito = manga.id in favoritosIds,
                                        onClick = { onMangaClick(manga) },
                                        onFavoritoClick = { favoritoViewModel.alternarFavorito(usuarioId, manga) }
                                    )
                                }
                            }
                        }
                    }

                    // Lançamentos (heurística: últimos itens do catálogo, ver HomeViewModel)
                    if (mangasLancamentos.isNotEmpty()) {
                        item {
                            SectionHeader(titulo = "🆕 Lançamentos", onVerTodosClick = onBuscaClick)
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = Spacing.md),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                items(mangasLancamentos, key = { "lancamento_${it.id}" }) { manga ->
                                    MangaCardFavoritavel(
                                        manga = manga,
                                        isFavorito = manga.id in favoritosIds,
                                        onClick = { onMangaClick(manga) },
                                        onFavoritoClick = { favoritoViewModel.alternarFavorito(usuarioId, manga) }
                                    )
                                }
                            }
                        }
                    }

                    // Recomendações: mesma categoria do mangá em destaque (ver HomeViewModel)
                    if (mangasRecomendados.isNotEmpty()) {
                        item {
                            SectionHeader(titulo = "✨ Você também pode gostar")
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = Spacing.md),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                items(mangasRecomendados, key = { "recomendado_${it.id}" }) { manga ->
                                    MangaCardFavoritavel(
                                        manga = manga,
                                        isFavorito = manga.id in favoritosIds,
                                        onClick = { onMangaClick(manga) },
                                        onFavoritoClick = { favoritoViewModel.alternarFavorito(usuarioId, manga) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarConfirmacaoLogout) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacaoLogout = false },
            title = { Text("Sair da conta") },
            text = { Text("Tem certeza que deseja sair?") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacaoLogout = false
                    onLogoutClick()
                }) {
                    Text("Sair")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacaoLogout = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Banner destacando um mangá em evidência no topo da Home.
 * Usa a mesma imagem/nome/preço já existentes no model Manga — não inventa
 * texto promocional ou imagem separada.
 */
@Composable
private fun BannerDestaque(manga: Manga, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .height(160.dp)
            .clip(RoundedCornerShape(Spacing.radiusLarge))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = manga.imagemUrl,
            contentDescription = manga.nome,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Spacing.md)
        ) {
            Text(
                text = "Em destaque",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = manga.nome,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "R$ ${"%.2f".format(manga.preco)}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltrosBottomSheet(
    categorias: List<String>,
    marcas: List<String>,
    categoriaSelecionada: String?,
    marcaSelecionada: String?,
    precoMinimo: Double?,
    precoMaximo: Double?,
    onCategoriaChange: (String?) -> Unit,
    onMarcaChange: (String?) -> Unit,
    onFaixaPrecoChange: (Double?, Double?) -> Unit,
    onLimpar: () -> Unit,
    onFechar: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var textoMin by remember(precoMinimo) { mutableStateOf(precoMinimo?.let { "%.2f".format(it) } ?: "") }
    var textoMax by remember(precoMaximo) { mutableStateOf(precoMaximo?.let { "%.2f".format(it) } ?: "") }

    ModalBottomSheet(onDismissRequest = onFechar, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
        ) {
            Text("Filtros", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(Spacing.md))
            Text("Categoria", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(Spacing.sm))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                item {
                    CategoryChip(
                        texto = "Todas",
                        selecionado = categoriaSelecionada == null,
                        onClick = { onCategoriaChange(null) }
                    )
                }
                items(categorias, key = { it }) { categoria ->
                    CategoryChip(
                        texto = categoria,
                        selecionado = categoriaSelecionada == categoria,
                        onClick = { onCategoriaChange(categoria) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
            Text("Marca", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(Spacing.sm))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                item {
                    CategoryChip(
                        texto = "Todas",
                        selecionado = marcaSelecionada == null,
                        onClick = { onMarcaChange(null) }
                    )
                }
                items(marcas, key = { it }) { marca ->
                    CategoryChip(
                        texto = marca,
                        selecionado = marcaSelecionada == marca,
                        onClick = { onMarcaChange(marca) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
            Text("Preço", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(
                    value = textoMin,
                    onValueChange = {
                        textoMin = it
                        onFaixaPrecoChange(it.replace(",", ".").toDoubleOrNull(), precoMaximo)
                    },
                    label = { Text("Mínimo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = textoMax,
                    onValueChange = {
                        textoMax = it
                        onFaixaPrecoChange(precoMinimo, it.replace(",", ".").toDoubleOrNull())
                    },
                    label = { Text("Máximo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                SecondaryButton(
                    text = "Limpar filtros",
                    onClick = {
                        textoMin = ""
                        textoMax = ""
                        onLimpar()
                    },
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = "Aplicar",
                    onClick = onFechar,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdenacaoBottomSheet(
    ordenacaoAtual: TipoOrdenacao,
    onSelecionar: (TipoOrdenacao) -> Unit,
    onFechar: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val opcoes = listOf(
        TipoOrdenacao.NENHUMA to "Mais relevantes",
        TipoOrdenacao.MENOR_PRECO to "Menor preço",
        TipoOrdenacao.MAIOR_PRECO to "Maior preço",
        TipoOrdenacao.MAIS_VENDIDOS to "Mais vendidos",
        TipoOrdenacao.MAIS_RECENTES to "Mais recentes",
        TipoOrdenacao.A_Z to "A-Z",
        TipoOrdenacao.Z_A to "Z-A"
    )

    ModalBottomSheet(onDismissRequest = onFechar, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
            Text("Ordenar por", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(Spacing.sm))

            opcoes.forEach { (tipo, rotulo) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelecionar(tipo) }
                        .padding(vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = ordenacaoAtual == tipo, onClick = { onSelecionar(tipo) })
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(rotulo, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}