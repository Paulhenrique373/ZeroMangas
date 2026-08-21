package com.example.zeromangas.ui.busca

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.ui.components.CategoryChip
import com.example.zeromangas.ui.components.EmptyState
import com.example.zeromangas.ui.components.LoadingState
import com.example.zeromangas.ui.components.MangaCardFavoritavel
import com.example.zeromangas.ui.home.FiltrosBottomSheet
import com.example.zeromangas.ui.home.OrdenacaoBottomSheet
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.viewmodel.FavoritoViewModel
import com.example.zeromangas.viewmodel.HomeViewModel

/**
 * Tela de busca completa (Etapa 4), acessada pela aba "Buscar".
 *
 * Reaproveita 100% da lógica já existente:
 * - [HomeViewModel] já tinha busca por texto, categoria, marca, faixa de preço
 *   e ordenação (mais barato, mais caro, A-Z, Z-A) rodando sobre o Supabase.
 * - [FiltrosBottomSheet] e [OrdenacaoBottomSheet] já existiam na Home e foram
 *   só importados aqui, sem duplicar nenhum código visual.
 *
 * A única coisa nova nesta tela é o filtro de "somente em estoque" (client-side,
 * já que o Manga já tem o campo `estoque`) e o histórico de pesquisas recentes,
 * guardado em memória durante a sessão.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuscaScreen(
    buscaViewModel: HomeViewModel = viewModel(),
    favoritoViewModel: FavoritoViewModel,
    usuarioId: String,
    onMangaClick: (Manga) -> Unit
) {
    val mangas by buscaViewModel.mangasFiltrados.collectAsState()
    val categorias by buscaViewModel.categorias.collectAsState()
    val marcas by buscaViewModel.marcas.collectAsState()
    val carregando by buscaViewModel.carregando.collectAsState()
    val erro by buscaViewModel.erro.collectAsState()
    val textoBusca by buscaViewModel.textoBusca.collectAsState()
    val categoriaSelecionada by buscaViewModel.categoriaSelecionada.collectAsState()
    val marcaSelecionada by buscaViewModel.marcaSelecionada.collectAsState()
    val precoMinimo by buscaViewModel.precoMinimo.collectAsState()
    val precoMaximo by buscaViewModel.precoMaximo.collectAsState()
    val ordenacao by buscaViewModel.ordenacao.collectAsState()
    val quantidadeFiltrosAtivos by buscaViewModel.quantidadeFiltrosAtivos.collectAsState()
    val favoritosIds by favoritoViewModel.favoritosIds.collectAsState()

    var mostrarFiltros by remember { mutableStateOf(false) }
    var mostrarOrdenacao by remember { mutableStateOf(false) }
    var somenteEmEstoque by remember { mutableStateOf(false) }

    // Histórico de pesquisas recentes (em memória, dura a sessão do app).
    val pesquisasRecentes = remember { mutableStateListOf<String>() }

    fun registrarPesquisa(termo: String) {
        val limpo = termo.trim()
        if (limpo.isBlank()) return
        pesquisasRecentes.remove(limpo)
        pesquisasRecentes.add(0, limpo)
        if (pesquisasRecentes.size > 6) {
            pesquisasRecentes.removeAt(pesquisasRecentes.lastIndex)
        }
    }

    val resultadosFinais = if (somenteEmEstoque) mangas.filter { it.estoque > 0 } else mangas
    val jaPesquisou = textoBusca.isNotBlank() || categoriaSelecionada != null || marcaSelecionada != null

    Column(modifier = Modifier.fillMaxSize()) {

        Text(
            text = "Buscar",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.md)
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        OutlinedTextField(
            value = textoBusca,
            onValueChange = { buscaViewModel.buscar(it) },
            placeholder = { Text("Buscar por nome, marca ou volume...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (textoBusca.isNotBlank()) {
                    IconButton(onClick = {
                        registrarPesquisa(textoBusca)
                        buscaViewModel.buscar("")
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(Spacing.radiusSmall),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        // ---- Filtros / Ordenar / Em estoque ----
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

        Spacer(modifier = Modifier.height(Spacing.sm))

        // ---- Disponibilidade ----
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryChip(
                texto = "📦 Somente em estoque",
                selecionado = somenteEmEstoque,
                onClick = { somenteEmEstoque = !somenteEmEstoque }
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // ---- Categorias ----
        if (categorias.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                items(categorias, key = { it }) { categoria ->
                    CategoryChip(
                        texto = categoria,
                        selecionado = categoriaSelecionada == categoria,
                        onClick = { buscaViewModel.selecionarCategoria(categoria) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))
        }

        when {
            carregando && mangas.isEmpty() -> LoadingState(modifier = Modifier.weight(1f))

            erro != null && mangas.isEmpty() -> EmptyState(
                titulo = erro ?: "",
                modifier = Modifier.weight(1f)
            )

            !jaPesquisou -> {
                Column(modifier = Modifier.weight(1f)) {
                    if (pesquisasRecentes.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Pesquisas recentes",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            items(pesquisasRecentes, key = { it }) { termo ->
                                CategoryChip(
                                    texto = termo,
                                    selecionado = false,
                                    onClick = { buscaViewModel.buscar(termo) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }
                    EmptyState(
                        titulo = "O que você quer ler hoje?",
                        subtitulo = "Digite um nome, marca ou escolha uma categoria acima.",
                        icone = Icons.Default.Search,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            resultadosFinais.isEmpty() -> EmptyState(
                titulo = "Nenhum resultado encontrado",
                subtitulo = "Tente outro termo, categoria ou filtro.",
                modifier = Modifier.weight(1f),
                textoAcao = if (quantidadeFiltrosAtivos > 0) "Limpar filtros" else null,
                onAcaoClick = if (quantidadeFiltrosAtivos > 0) {
                    { buscaViewModel.limparFiltros() }
                } else null
            )

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = Spacing.md,
                        end = Spacing.md,
                        top = Spacing.sm,
                        bottom = Spacing.xl
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.weight(1f)
                ) {
                    items(resultadosFinais, key = { it.id }) { manga ->
                        MangaCardFavoritavel(
                            manga = manga,
                            isFavorito = manga.id in favoritosIds,
                            onClick = {
                                registrarPesquisa(textoBusca)
                                onMangaClick(manga)
                            },
                            onFavoritoClick = { favoritoViewModel.alternarFavorito(usuarioId, manga) }
                        )
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
            onCategoriaChange = { buscaViewModel.definirCategoria(it) },
            onMarcaChange = { buscaViewModel.definirMarca(it) },
            onFaixaPrecoChange = { min, max -> buscaViewModel.definirFaixaDePreco(min, max) },
            onLimpar = { buscaViewModel.limparFiltros() },
            onFechar = { mostrarFiltros = false }
        )
    }

    if (mostrarOrdenacao) {
        OrdenacaoBottomSheet(
            ordenacaoAtual = ordenacao,
            onSelecionar = {
                buscaViewModel.ordenarPor(it)
                mostrarOrdenacao = false
            },
            onFechar = { mostrarOrdenacao = false }
        )
    }
}