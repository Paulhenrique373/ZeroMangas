package com.example.zeromangas.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapVert
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
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.viewmodel.CartViewModel
import com.example.zeromangas.viewmodel.FavoritoViewModel
import com.example.zeromangas.viewmodel.HomeViewModel
import com.example.zeromangas.viewmodel.TipoOrdenacao

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    cartViewModel: CartViewModel,
    favoritoViewModel: FavoritoViewModel,
    usuarioId: String,
    onMangaClick: (Manga) -> Unit = {},
    onCarrinhoClick: () -> Unit = {},
    onPedidosClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    onFavoritosClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val mangas by homeViewModel.mangasFiltrados.collectAsState()
    val mangasEmDestaque by homeViewModel.mangasEmDestaque.collectAsState()
    val categorias by homeViewModel.categorias.collectAsState()
    val marcas by homeViewModel.marcas.collectAsState()
    val carregando by homeViewModel.carregando.collectAsState()
    val erro by homeViewModel.erro.collectAsState()
    val textoBusca by homeViewModel.textoBusca.collectAsState()
    val categoriaSelecionada by homeViewModel.categoriaSelecionada.collectAsState()
    val marcaSelecionada by homeViewModel.marcaSelecionada.collectAsState()
    val precoMinimo by homeViewModel.precoMinimo.collectAsState()
    val precoMaximo by homeViewModel.precoMaximo.collectAsState()
    val ordenacao by homeViewModel.ordenacao.collectAsState()
    val quantidadeFiltrosAtivos by homeViewModel.quantidadeFiltrosAtivos.collectAsState()
    val itensCarrinho by cartViewModel.itens.collectAsState()
    val quantidadeNoCarrinho = itensCarrinho.sumOf { it.quantidade }
    val favoritosIds by favoritoViewModel.favoritosIds.collectAsState()

    var mostrarFiltros by remember { mutableStateOf(false) }
    var mostrarOrdenacao by remember { mutableStateOf(false) }
    var mostrarConfirmacaoLogout by remember { mutableStateOf(false) }

    LaunchedEffect(usuarioId) {
        favoritoViewModel.carregarFavoritos(usuarioId)
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
                    text = "ZeroMangás",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
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
        OutlinedTextField(
            value = textoBusca,
            onValueChange = { homeViewModel.buscar(it) },
            placeholder = { Text("Buscar por nome, marca ou volume...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(Spacing.radiusSmall),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        // ---- Filtros / Ordenar ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            OutlinedButton(
                onClick = { mostrarFiltros = true },
                shape = RoundedCornerShape(Spacing.radiusSmall),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (quantidadeFiltrosAtivos > 0) "Filtros ($quantidadeFiltrosAtivos)" else "Filtros")
            }
            OutlinedButton(
                onClick = { mostrarOrdenacao = true },
                shape = RoundedCornerShape(Spacing.radiusSmall),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ordenar")
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // ---- Categorias ----
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            items(categorias, key = { it }) { categoria ->
                CategoryChip(
                    texto = categoria,
                    selecionado = categoriaSelecionada == categoria,
                    onClick = { homeViewModel.selecionarCategoria(categoria) }
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // ---- Conteúdo principal ----
        when {
            carregando && mangas.isEmpty() -> {
                LoadingState(modifier = Modifier.weight(1f))
            }

            erro != null && mangas.isEmpty() -> {
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

            mangas.isEmpty() -> {
                EmptyState(
                    titulo = "Nenhum mangá encontrado",
                    subtitulo = "Tente ajustar sua busca ou seus filtros.",
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
                            SectionHeader(titulo = "🔥 Mais vendidos")
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

                    item {
                        SectionHeader(titulo = "Catálogo")
                    }

                    // Catálogo completo, em linhas de 2 cards
                    items(mangas.chunked(2), key = { row -> row.joinToString { it.id } }) { linha ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            linha.forEach { manga ->
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

    if (mostrarFiltros) {
        FiltrosBottomSheet(
            categorias = categorias,
            marcas = marcas,
            categoriaSelecionada = categoriaSelecionada,
            marcaSelecionada = marcaSelecionada,
            precoMinimo = precoMinimo,
            precoMaximo = precoMaximo,
            onCategoriaChange = { homeViewModel.definirCategoria(it) },
            onMarcaChange = { homeViewModel.definirMarca(it) },
            onFaixaPrecoChange = { min, max -> homeViewModel.definirFaixaDePreco(min, max) },
            onLimpar = { homeViewModel.limparFiltros() },
            onFechar = { mostrarFiltros = false }
        )
    }

    if (mostrarOrdenacao) {
        OrdenacaoBottomSheet(
            ordenacaoAtual = ordenacao,
            onSelecionar = {
                homeViewModel.ordenarPor(it)
                mostrarOrdenacao = false
            },
            onFechar = { mostrarOrdenacao = false }
        )
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
        TipoOrdenacao.NENHUMA to "Relevância",
        TipoOrdenacao.MENOR_PRECO to "Menor preço",
        TipoOrdenacao.MAIOR_PRECO to "Maior preço",
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