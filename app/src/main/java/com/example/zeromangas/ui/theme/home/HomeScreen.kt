package com.example.zeromangas.ui.theme.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
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
import com.example.zeromangas.ui.components.formatarPrecoBr
import com.example.zeromangas.viewmodel.FavoritoViewModel
import com.example.zeromangas.viewmodel.HomeViewModel
import com.example.zeromangas.viewmodel.NotificacaoViewModel
import com.example.zeromangas.viewmodel.TipoOrdenacao

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    favoritoViewModel: FavoritoViewModel,
    notificacaoViewModel: NotificacaoViewModel,
    usuarioId: String,
    onMangaClick: (Manga) -> Unit = {},
    onBuscaClick: () -> Unit = {},
    onNotificacoesClick: () -> Unit = {},
    quantidadeNoCarrinho: Int = 0,
    onCarrinhoClick: () -> Unit = {}
) {
    val mangasEmDestaque by homeViewModel.mangasEmDestaque.collectAsState()
    val mangasLancamentos by homeViewModel.mangasLancamentos.collectAsState()
    val mangasRecomendados by homeViewModel.mangasRecomendados.collectAsState()
    val categorias by homeViewModel.categorias.collectAsState()
    val carregando by homeViewModel.carregando.collectAsState()
    val erro by homeViewModel.erro.collectAsState()
    val categoriaSelecionada by homeViewModel.categoriaSelecionada.collectAsState()
    val favoritosIds by favoritoViewModel.favoritosIds.collectAsState()
    val quantidadeNotificacoesNaoLidas by notificacaoViewModel.quantidadeNaoLidas.collectAsState()

    LaunchedEffect(usuarioId) {
        favoritoViewModel.carregarFavoritos(usuarioId)
        notificacaoViewModel.iniciar(usuarioId)
    }

    when {
            carregando && mangasEmDestaque.isEmpty() && mangasLancamentos.isEmpty() -> {
                LoadingState()
            }

            erro != null && mangasEmDestaque.isEmpty() && mangasLancamentos.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
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
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = Spacing.lg, bottom = Spacing.xxl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sectionGap)
                ) {
                    item {
                        HomeHeader(
                            quantidadeNotificacoes = quantidadeNotificacoesNaoLidas,
                            quantidadeNoCarrinho = quantidadeNoCarrinho,
                            onNotificacoesClick = onNotificacoesClick,
                            onCarrinhoClick = onCarrinhoClick
                        )
                    }
                    item { HomeSearchField(onClick = onBuscaClick) }
                    if (categorias.isNotEmpty()) {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
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
                        }
                    }
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
                                contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap)
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
                                contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap)
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
                            SectionHeader(titulo = "✨ Recomendados para você", onVerTodosClick = onBuscaClick)
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap)
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

@Composable
private fun HomeHeader(
    quantidadeNotificacoes: Int,
    quantidadeNoCarrinho: Int,
    onNotificacoesClick: () -> Unit,
    onCarrinhoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Olá! 👋",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Encontre sua próxima história",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BadgedBox(badge = { if (quantidadeNotificacoes > 0) Badge { Text(quantidadeNotificacoes.toString()) } }) {
                IconButton(onClick = onNotificacoesClick) {
                    Icon(Icons.Default.Notifications, "Notificações", tint = MaterialTheme.colorScheme.primary)
                }
            }
            BadgedBox(badge = { if (quantidadeNoCarrinho > 0) Badge { Text(quantidadeNoCarrinho.toString()) } }) {
                IconButton(onClick = onCarrinhoClick) {
                    Icon(Icons.Default.ShoppingCart, "Carrinho", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun HomeSearchField(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal)
            .height(Spacing.textFieldMinHeight)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface)
            .border(Spacing.borderWidth, BordaSutil, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = "Buscar mangás, autores ou gêneros...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            .padding(horizontal = Spacing.screenHorizontal)
            .height(Spacing.bannerHeight)
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
                .padding(Spacing.lg)
        ) {
            Text(
                text = "Em destaque",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primaryContainer
            )
            Text(
                text = manga.nome,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatarPrecoBr(manga.preco),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Text(
                    text = "Ver detalhes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            }
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
