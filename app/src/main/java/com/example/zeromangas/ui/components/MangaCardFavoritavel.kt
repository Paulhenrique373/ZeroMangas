package com.example.zeromangas.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.unit.dp
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
    onAdicionarAoCarrinho: (() -> Unit)? = null
) {
    val escalaCoracao by animateFloatAsState(
        targetValue = if (isFavorito) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "escalaFavorito"
    )

    Box {
        MangaCard(manga = manga, onClick = onClick)

        Box(
            modifier = Modifier
                .padding(Spacing.xs)
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .clickable { onFavoritoClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorito) "Remover dos favoritos" else "Adicionar aos favoritos",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(18.dp)
                    .scale(escalaCoracao)
            )
        }

        if (onAdicionarAoCarrinho != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.xs)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .clickable { onAdicionarAoCarrinho() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AddShoppingCart,
                    contentDescription = "Adicionar ao carrinho",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}