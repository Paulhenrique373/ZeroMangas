package com.example.zeromangas.ui.detalhes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zeromangas.data.model.Manga

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalhesScreen(
    manga: Manga?,
    onVoltar: () -> Unit,
    onAdicionarAoCarrinho: (Manga) -> Unit
) {
    if (manga == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Manga não encontrado")
        }
        return
    }

    val esgotado = manga.estoque <= 0
    val estoqueBaixo = manga.estoque in 1..5

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
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AsyncImage(
                    model = manga.imagemUrl,
                    contentDescription = manga.nome,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (manga.emDestaque) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.tertiary)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("🔥 Destaque", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (esgotado) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.error)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Esgotado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = manga.marca,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = manga.nome,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                AssistChip(onClick = {}, label = { Text(manga.categoria) })
                Spacer(modifier = Modifier.width(8.dp))
                AssistChip(onClick = {}, label = { Text("Vol. ${manga.volume}") })
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "R$ ${"%.2f".format(manga.preco)}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (esgotado) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Produto esgotado no momento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (estoqueBaixo) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Últimas ${manga.estoque} unidades!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Descrição",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = manga.descricao.ifBlank { "Sem descrição disponível." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = { onAdicionarAoCarrinho(manga) },
            enabled = !esgotado,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(if (esgotado) "Produto esgotado" else "Adicionar ao Carrinho")
        }
    }
}