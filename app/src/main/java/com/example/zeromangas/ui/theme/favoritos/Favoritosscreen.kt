package com.example.zeromangas.ui.theme.favoritos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.ui.components.ErrorState
import com.example.zeromangas.ui.components.EmptyState
import com.example.zeromangas.ui.components.MangaCardFavoritavel
import com.example.zeromangas.ui.components.MangaCardSkeleton
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.viewmodel.CartViewModel
import com.example.zeromangas.viewmodel.FavoritoViewModel
import com.example.zeromangas.viewmodel.FavoritosState
import kotlinx.coroutines.delay

/**
 * Tela de favoritos. A lógica (carregar/alternar favorito) continua 100% no
 * [FavoritoViewModel] já existente — só o visual passou a usar o design system
 * (EmptyState, espaçamentos padronizados). Novidade: agora dá pra adicionar direto
 * ao carrinho pelo próprio card, usando o [CartViewModel] (mesma lógica de adicionarItem
 * já usada no carrinho e nos detalhes do mangá).
 */
@Composable
fun FavoritosScreen(
    favoritoViewModel: FavoritoViewModel,
    cartViewModel: CartViewModel,
    usuarioId: String,
    onVoltar: () -> Unit,
    onMangaClick: (Manga) -> Unit,
    onExplorarClick: () -> Unit = {}
) {
    val mangasFavoritos by favoritoViewModel.mangasFavoritos.collectAsState()
    val favoritosState by favoritoViewModel.favoritosState.collectAsState()
    val avisoEstoque by cartViewModel.avisoEstoque.collectAsState()

    LaunchedEffect(usuarioId) {
        favoritoViewModel.carregarFavoritos(usuarioId)
    }

    LaunchedEffect(avisoEstoque) {
        if (avisoEstoque != null) {
            delay(3000)
            cartViewModel.limparAvisoEstoque()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Meus favoritos", style = MaterialTheme.typography.headlineMedium, color = TextoPrincipal)
                Text(
                    text = when (mangasFavoritos.size) {
                        0 -> "Sua biblioteca pessoal"
                        1 -> "1 mangá salvo"
                        else -> "${mangasFavoritos.size} mangás salvos"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Mantém o retorno de estoque que já existia nesta tela, agora em um
        // aviso compacto para não disputar atenção com a biblioteca.
        avisoEstoque?.let { mensagem ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal)
            ) {
                Text(
                    text = mensagem,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(Spacing.md)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        when (favoritosState) {
            FavoritosState.Idle, FavoritosState.Carregando -> FavoritosSkeletonGrid(
                modifier = Modifier.weight(1f)
            )
            is FavoritosState.Erro -> ErrorState(
                titulo = "Não foi possível carregar favoritos",
                subtitulo = (favoritosState as FavoritosState.Erro).mensagem,
                textoAcao = "Tentar novamente",
                onAcaoClick = { favoritoViewModel.carregarFavoritos(usuarioId) },
                modifier = Modifier.weight(1f)
            )
            FavoritosState.Sucesso -> if (mangasFavoritos.isEmpty()) {
                EmptyState(
                titulo = "Você ainda não favoritou nenhum mangá",
                subtitulo = "Salve histórias que você quer ler para encontrá-las sempre aqui.",
                icone = Icons.Outlined.FavoriteBorder,
                textoAcao = "Explorar catálogo",
                onAcaoClick = onExplorarClick,
                modifier = Modifier.weight(1f)
                )
            } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = Spacing.screenHorizontal,
                    end = Spacing.screenHorizontal,
                    top = Spacing.xs,
                    bottom = Spacing.xxl
                ),
                horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                modifier = Modifier.weight(1f)
            ) {
                items(mangasFavoritos, key = { it.id }) { manga ->
                    MangaCardFavoritavel(
                        manga = manga,
                        onClick = { onMangaClick(manga) },
                        isFavorito = true,
                        onFavoritoClick = { favoritoViewModel.alternarFavorito(usuarioId, manga) },
                        onAdicionarAoCarrinho = { cartViewModel.adicionarItem(manga) },
                        preencherLargura = true
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun FavoritosSkeletonGrid(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        items(6) { MangaCardSkeleton(preencherLargura = true) }
    }
}
