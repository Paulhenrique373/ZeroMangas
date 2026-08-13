package com.example.zeromangas.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.zeromangas.data.model.Manga
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ZeroMangás",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

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

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = textoBusca,
            onValueChange = { homeViewModel.buscar(it) },
            placeholder = { Text("Buscar por nome, marca ou volume...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(onClick = { mostrarFiltros = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (quantidadeFiltrosAtivos > 0) "Filtros ($quantidadeFiltrosAtivos)" else "Filtros")
            }
            OutlinedButton(onClick = { mostrarOrdenacao = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ordenar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(homeViewModel.categorias) { categoria ->
                FiltroChip(
                    texto = categoria,
                    selecionado = categoriaSelecionada == categoria,
                    onClick = { homeViewModel.selecionarCategoria(categoria) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (mangas.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum mangá encontrado.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 24.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(mangas) { manga ->
                    MangaCard(
                        manga = manga,
                        onClick = { onMangaClick(manga) },
                        isFavorito = manga.id in favoritosIds,
                        onFavoritoClick = { favoritoViewModel.alternarFavorito(usuarioId, manga) }
                    )
                }
            }
        }
    }

    if (mostrarFiltros) {
        FiltrosBottomSheet(
            categorias = homeViewModel.categorias,
            marcas = homeViewModel.marcas,
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text("Filtros", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))
            Text("Categoria", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FiltroChip(
                        texto = "Todas",
                        selecionado = categoriaSelecionada == null,
                        onClick = { onCategoriaChange(null) }
                    )
                }
                items(categorias) { categoria ->
                    FiltroChip(
                        texto = categoria,
                        selecionado = categoriaSelecionada == categoria,
                        onClick = { onCategoriaChange(categoria) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Marca", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FiltroChip(
                        texto = "Todas",
                        selecionado = marcaSelecionada == null,
                        onClick = { onMarcaChange(null) }
                    )
                }
                items(marcas) { marca ->
                    FiltroChip(
                        texto = marca,
                        selecionado = marcaSelecionada == marca,
                        onClick = { onMarcaChange(marca) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Preço", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        textoMin = ""
                        textoMax = ""
                        onLimpar()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Limpar filtros")
                }
                Button(onClick = onFechar, modifier = Modifier.weight(1f)) {
                    Text("Aplicar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Ordenar por", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))

            opcoes.forEach { (tipo, rotulo) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelecionar(tipo) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = ordenacaoAtual == tipo, onClick = { onSelecionar(tipo) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(rotulo, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun FiltroChip(texto: String, selecionado: Boolean, onClick: () -> Unit) {
    val corFundo = if (selecionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val corTexto = if (selecionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(corFundo)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = texto, color = corTexto, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun MangaCard(
    manga: Manga,
    onClick: () -> Unit = {},
    isFavorito: Boolean = false,
    onFavoritoClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (manga.imagemUrl.isNotBlank()) {
                AsyncImage(
                    model = manga.imagemUrl,
                    contentDescription = manga.nome,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = "📖",
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            if (manga.emDestaque) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.tertiary)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("🔥", style = MaterialTheme.typography.labelSmall)
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .clickable { onFavoritoClick() }
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = if (isFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorito) "Remover dos favoritos" else "Adicionar aos favoritos",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = manga.marca,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = manga.nome,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "R$ ${"%.2f".format(manga.preco)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}