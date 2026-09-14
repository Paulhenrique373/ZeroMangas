package com.example.zeromangas.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.ui.theme.BordaSutil
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.VermelhoErro

enum class MangaCardState { NORMAL, PRESSIONADO, LOADING, INDISPONIVEL }

/**
 * Card vertical de mangá, usado em LazyRows (Mais vendidos, Lançamentos) e grids
 * de resultado de busca/categoria. Mostra capa, nome, volume e preço.
 *
 * Não inclui lógica de favoritos/carrinho — isso fica a cargo de quem usa o card,
 * via [onClick], para não acoplar o componente visual a um ViewModel específico.
 *
 * ETAPA 11 (polimento): o card agora "encolhe" sutilmente enquanto está sendo
 * pressionado (mesma técnica usada em [PrimaryButton]/[SecondaryButton]), dando
 * feedback visual de toque antes mesmo da navegação para os detalhes acontecer.
 */
@Composable
fun MangaCard(
    manga: Manga,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    precoAntigo: Double? = null,
    state: MangaCardState = if (manga.estoque <= 0) MangaCardState.INDISPONIVEL else MangaCardState.NORMAL,
    preencherLargura: Boolean = false
) {
    if (state == MangaCardState.LOADING) {
        MangaCardSkeleton(modifier = modifier, preencherLargura = preencherLargura)
        return
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressionado by interactionSource.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (pressionado || state == MangaCardState.PRESSIONADO) 0.98f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "escalaMangaCard"
    )

    Column(
        modifier = modifier
            .then(if (preencherLargura) Modifier.fillMaxWidth() else Modifier.width(Spacing.mangaCoverWidth))
            .scale(escala)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(Spacing.subtleElevation, RoundedCornerShape(Spacing.radiusMedium))
                .clip(RoundedCornerShape(Spacing.radiusMedium))
                .background(FundoCard)
                .border(Spacing.borderWidth, BordaSutil, RoundedCornerShape(Spacing.radiusMedium))
        ) {
            AsyncImage(
                model = manga.imagemUrl,
                contentDescription = manga.nome,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatioCapa()
            )

            if (manga.emPromocao && manga.precoPromocional != null && manga.precoPromocional < manga.preco) {
                ProductBadge(text = "Oferta", modifier = Modifier.align(Alignment.TopStart).padding(Spacing.xs))
            }
            if (state == MangaCardState.INDISPONIVEL) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.xs)
                        .clip(RoundedCornerShape(Spacing.radiusSmall))
                        .background(VermelhoErro)
                        .padding(horizontal = Spacing.sm, vertical = 2.dp)
                ) {
                    Text(
                        text = "Indisponível",
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = Spacing.sm)
                .height(42.dp)
        )
        if (!manga.nome.contains(Regex("\\bvol\\.?\\s*${manga.volume}\\b", RegexOption.IGNORE_CASE))) {
            Text(
                text = "Vol. ${manga.volume}",
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario
            )
        } else {
            Text(
                text = manga.marca.ifBlank { "Mangá" },
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario
            )
        }
        if (manga.totalAvaliacoes > 0) {
            RatingStars(
                nota = manga.notaMedia,
                tamanhoEstrela = Spacing.iconSmall,
                modifier = Modifier.padding(top = Spacing.xs)
            )
        }
        val precoAtual = manga.precoPromocional?.takeIf { manga.emPromocao && it < manga.preco } ?: manga.preco
        PriceText(
            preco = precoAtual,
            precoAntigo = precoAntigo ?: manga.preco.takeIf { precoAtual < it },
            modifier = Modifier.padding(top = Spacing.xs)
        )
    }
}

@Composable
private fun ProductBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Spacing.radiusPill))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
    }
}

/** Placeholder estrutural usado enquanto uma grade de catálogo é carregada. */
@Composable
fun MangaCardSkeleton(modifier: Modifier = Modifier, preencherLargura: Boolean = false) {
    Column(modifier = modifier.then(if (preencherLargura) Modifier.fillMaxWidth() else Modifier.width(Spacing.mangaCoverWidth))) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.mangaCoverHeight)
                .clip(RoundedCornerShape(Spacing.radiusMedium))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Box(modifier = Modifier.fillMaxWidth(0.82f).height(14.dp).clip(RoundedCornerShape(Spacing.radiusPill)).background(MaterialTheme.colorScheme.surfaceVariant))
        Spacer(modifier = Modifier.height(Spacing.xs))
        Box(modifier = Modifier.fillMaxWidth(0.45f).height(12.dp).clip(RoundedCornerShape(Spacing.radiusPill)).background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

/**
 * Proporção de capa de mangá (aprox. formato de livro, 2:3).
 * Extraída como extension pra manter o height consistente sem repetir número mágico.
 */
private fun Modifier.aspectRatioCapa(): Modifier =
    this.then(Modifier.aspectRatio(2f / 3f))
