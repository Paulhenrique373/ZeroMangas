package com.example.zeromangas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.ui.theme.Spacing

/**
 * Envolve o [MangaCard] do design system com um ícone de favorito sobreposto,
 * já que o componente compartilhado não tem essa opção embutida.
 * Usado na Home e na tela de Favoritos.
 */
@Composable
fun MangaCardFavoritavel(
    manga: Manga,
    isFavorito: Boolean,
    onClick: () -> Unit,
    onFavoritoClick: () -> Unit
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
    }
}