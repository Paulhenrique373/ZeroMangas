package com.example.zeromangas.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 */
@Composable
fun MangaCardFavoritavel(
    manga: Manga,
    isFavorito: Boolean,
    onClick: () -> Unit,
    onFavoritoClick: () -> Unit,
    onAdicionarAoCarrinho: (() -> Unit)? = null
) {
    Box {
        MangaCard(manga = manga, onClick = onClick)

        Box(
            modifier = Modifier
                .padding(Spacing.xs)
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

        if (onAdicionarAoCarrinho != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.xs)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .clickable { onAdicionarAoCarrinho() }
                    .padding(6.dp)
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