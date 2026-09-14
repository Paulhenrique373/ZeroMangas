package com.example.zeromangas.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.ui.theme.Spacing

/**
 * Envolve o [MangaCard] do design system com um ícone de favorito sobreposto,
 * já que o componente compartilhado não tem essa opção embutida.
 * Usado na Home, Busca, Detalhes e Favoritos.
 *
 * [onAdicionarAoCarrinho] é opcional: quando informado (hoje só na tela de Favoritos),
 * mostra um segundo botão circular no canto inferior para adicionar ao carrinho direto
 * do card, sem precisar abrir os detalhes do mangá. Nas outras telas continua null e o
 * visual fica exatamente igual a antes.
 *
 * ETAPA 11 (polimento): o coração agora dá um pequeno "pulo" (spring) sempre que
 * [isFavorito] passa a ser true, reforçando visualmente a ação de favoritar sem
 * precisar de nenhum Snackbar/Toast extra. Os círculos de toque (favoritar/adicionar
 * ao carrinho) também cresceram de ~30dp para 36dp — mais perto do mínimo de 48dp
 * recomendado pra acessibilidade, sem ficar desproporcional num card de 130dp de largura.
 */
@Composable
fun MangaCardFavoritavel(
    manga: Manga,
    isFavorito: Boolean,
    onClick: () -> Unit,
    onFavoritoClick: () -> Unit,
    onAdicionarAoCarrinho: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    state: MangaCardState = if (manga.estoque <= 0) MangaCardState.INDISPONIVEL else MangaCardState.NORMAL,
    preencherLargura: Boolean = false
) {
    if (state == MangaCardState.LOADING) {
        MangaCardSkeleton(modifier = modifier, preencherLargura = preencherLargura)
        return
    }
    val escalaCoracao by animateFloatAsState(
        targetValue = if (isFavorito) 1.08f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "escalaFavorito"
    )

    Box(modifier = modifier) {
        MangaCard(manga = manga, onClick = onClick, state = state, preencherLargura = preencherLargura)

        Box(
            modifier = Modifier
                .padding(Spacing.xs)
                .size(Spacing.touchTarget)
                .clickable { onFavoritoClick() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(Spacing.overlayVisualSize).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorito) "Remover dos favoritos" else "Adicionar aos favoritos",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Spacing.iconSmall).scale(escalaCoracao)
                )
            }
        }

        if (onAdicionarAoCarrinho != null) {
            val esgotado = state == MangaCardState.INDISPONIVEL
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.xs)
                    .size(Spacing.touchTarget)
                    .clickable(enabled = !esgotado) { onAdicionarAoCarrinho() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(Spacing.overlayVisualSize).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = if (esgotado) "Produto esgotado" else "Adicionar ao carrinho",
                        tint = if (esgotado) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Spacing.iconSmall)
                    )
                }
            }
        }
    }
}
