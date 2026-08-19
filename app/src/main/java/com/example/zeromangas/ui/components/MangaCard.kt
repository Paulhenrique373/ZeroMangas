package com.example.zeromangas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.VermelhoErro

/**
 * Card vertical de mangá, usado em LazyRows (Mais vendidos, Lançamentos) e grids
 * de resultado de busca/categoria. Mostra capa, nome, volume e preço.
 *
 * Não inclui lógica de favoritos/carrinho — isso fica a cargo de quem usa o card,
 * via [onClick], para não acoplar o componente visual a um ViewModel específico.
 */
@Composable
fun MangaCard(
    manga: Manga,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    precoAntigo: Double? = null
) {
    Column(
        modifier = modifier
            .width(Spacing.mangaCoverWidth)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(Spacing.mangaCoverWidth)
                .clip(RoundedCornerShape(Spacing.radiusMedium))
                .background(FundoCard)
        ) {
            AsyncImage(
                model = manga.imagemUrl,
                contentDescription = manga.nome,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatioCapa()
            )

            if (manga.estoque <= 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.xs)
                        .clip(RoundedCornerShape(Spacing.radiusSmall))
                        .background(VermelhoErro)
                        .padding(horizontal = Spacing.sm, vertical = 2.dp)
                ) {
                    Text(
                        text = "Esgotado",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoPrincipal
                    )
                }
            }
        }

        Text(
            text = manga.nome,
            style = MaterialTheme.typography.bodyMedium,
            color = TextoPrincipal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.sm)
        )
        Text(
            text = "Vol. ${manga.volume}",
            style = MaterialTheme.typography.labelSmall,
            color = TextoSecundario
        )
        PriceText(
            preco = manga.preco,
            precoAntigo = precoAntigo,
            modifier = Modifier.padding(top = Spacing.xs)
        )
    }
}

/**
 * Proporção de capa de mangá (aprox. formato de livro, 2:3).
 * Extraída como extension pra manter o height consistente sem repetir número mágico.
 */
private fun Modifier.aspectRatioCapa(): Modifier =
    this.then(Modifier.height(Spacing.mangaCoverHeight))
