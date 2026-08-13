package com.example.zeromangas.ui.favoritos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.ui.home.MangaCard
import com.example.zeromangas.viewmodel.FavoritoViewModel

@Composable
fun FavoritosScreen(
    favoritoViewModel: FavoritoViewModel,
    usuarioId: String,
    onVoltar: () -> Unit,
    onMangaClick: (Manga) -> Unit
) {
    val mangasFavoritos by favoritoViewModel.mangasFavoritos.collectAsState()

    LaunchedEffect(usuarioId) {
        favoritoViewModel.carregarFavoritos(usuarioId)
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoltar) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
            }
            Text(
                text = "Meus Favoritos",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (mangasFavoritos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Você ainda não favoritou nenhum mangá.\nToque no ❤ de um mangá para adicioná-lo aqui.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(mangasFavoritos, key = { it.id }) { manga ->
                    MangaCard(
                        manga = manga,
                        onClick = { onMangaClick(manga) },
                        isFavorito = true,
                        onFavoritoClick = { favoritoViewModel.alternarFavorito(usuarioId, manga) }
                    )
                }
            }
        }
    }
}