package com.example.zeromangas.ui.theme.favoritos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.ui.components.EmptyState
import com.example.zeromangas.ui.components.MangaCardFavoritavel
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.viewmodel.CartViewModel
import com.example.zeromangas.viewmodel.FavoritoViewModel
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
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoltar) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextoPrincipal)
            }
            Text(
                text = "Meus Favoritos",
                style = MaterialTheme.typography.titleLarge,
                color = TextoPrincipal,
                modifier = Modifier.padding(start = Spacing.sm)
            )
        }

        if (avisoEstoque != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .clip(RoundedCornerShape(Spacing.radiusSmall))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(Spacing.md)
            ) {
                Text(
                    text = avisoEstoque ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        if (mangasFavoritos.isEmpty()) {
            EmptyState(
                titulo = "Você ainda não favoritou nenhum mangá",
                subtitulo = "Toque no ❤ de um mangá para adicioná-lo aqui.",
                icone = Icons.Outlined.FavoriteBorder,
                textoAcao = "Explorar catálogo",
                onAcaoClick = onExplorarClick
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = Spacing.md,
                    end = Spacing.md,
                    top = Spacing.sm,
                    bottom = Spacing.lg
                ),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.weight(1f)
            ) {
                items(mangasFavoritos, key = { it.id }) { manga ->
                    MangaCardFavoritavel(
                        manga = manga,
                        onClick = { onMangaClick(manga) },
                        isFavorito = true,
                        onFavoritoClick = { favoritoViewModel.alternarFavorito(usuarioId, manga) },
                        onAdicionarAoCarrinho = { cartViewModel.adicionarItem(manga) }
                    )
                }
            }
        }
    }
}