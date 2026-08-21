package com.example.zeromangas.ui.theme.detalhes

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.ui.components.EmptyState
import com.example.zeromangas.ui.components.MangaCardFavoritavel
import com.example.zeromangas.ui.components.PriceText
import com.example.zeromangas.ui.components.PrimaryButton
import com.example.zeromangas.ui.components.SectionHeader
import com.example.zeromangas.ui.theme.AmareloDestaque
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.RoxoNeonClaro
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.viewmodel.FavoritoViewModel

/**
 * Tela de detalhes de um mangá.
 *
 * Reaproveita os componentes do design system (PriceText, PrimaryButton, SectionHeader,
 * MangaCardFavoritavel) já usados na Home/Busca/Favoritos, em vez de recriar visual novo.
 *
 * Observação: o model [Manga] não possui campos de autor, páginas ou avaliação (nota),
 * então essas informações não são exibidas aqui para não inventar dados que não existem
 * no banco. Se esses campos forem adicionados futuramente na tabela "produtos" do
 * Supabase, é só estendê-los aqui e usar o componente RatingStars já existente.
 */
@Composable
fun DetalhesScreen(
    manga: Manga?,
    favoritoViewModel: FavoritoViewModel,
    usuarioId: String,
    onVoltar: () -> Unit,
    onAdicionarAoCarrinho: (Manga) -> Unit,
    recomendados: List<Manga> = emptyList(),
    onMangaClick: (Manga) -> Unit = {}
) {
    if (manga == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BotaoCircular(icone = Icons.Default.ArrowBack, contentDescription = "Voltar", onClick = onVoltar)
            }
            EmptyState(
                titulo = "Mangá não encontrado",
                subtitulo = "Ele pode ter sido removido do catálogo."
            )
        }
        return
    }

    val favoritosIds by favoritoViewModel.favoritosIds.collectAsState()
    val isFavorito = manga.id in favoritosIds

    // ETAPA 11 (polimento): mesmo "pulo" do coração usado no MangaCardFavoritavel,
    // aqui aplicado ao botão de favorito grande sobre a capa.
    val escalaFavorito by animateFloatAsState(
        targetValue = if (isFavorito) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "escalaFavoritoDetalhes"
    )

    LaunchedEffect(usuarioId) {
        favoritoViewModel.carregarFavoritos(usuarioId)
    }

    val esgotado = manga.estoque <= 0
    val estoqueBaixo = manga.estoque in 1..5

    Column(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {

            // ---------- Capa em destaque (full-bleed) ----------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(bottomStart = Spacing.radiusLarge, bottomEnd = Spacing.radiusLarge))
                    .background(FundoCard)
            ) {
                AsyncImage(
                    model = manga.imagemUrl,
                    contentDescription = manga.nome,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BotaoCircular(icone = Icons.Default.ArrowBack, contentDescription = "Voltar", onClick = onVoltar)
                    BotaoCircular(
                        icone = if (isFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorito) "Remover dos favoritos" else "Adicionar aos favoritos",
                        tint = if (isFavorito) RoxoNeonClaro else TextoPrincipal,
                        escala = escalaFavorito,
                        onClick = { favoritoViewModel.alternarFavorito(usuarioId, manga) }
                    )
                }

                if (manga.emDestaque) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(Spacing.md)
                            .clip(RoundedCornerShape(Spacing.radiusSmall))
                            .background(MaterialTheme.colorScheme.tertiary)
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                    ) {
                        Text("🔥 Destaque", style = MaterialTheme.typography.labelSmall, color = TextoPrincipal)
                    }
                }

                if (esgotado) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(Spacing.md)
                            .clip(RoundedCornerShape(Spacing.radiusSmall))
                            .background(MaterialTheme.colorScheme.error)
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                    ) {
                        Text(
                            "Esgotado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }

            // ---------- Informações principais ----------
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg)) {

                Spacer(modifier = Modifier.height(Spacing.md))

                Text(
                    text = manga.marca.ifBlank { "Editora não informada" },
                    style = MaterialTheme.typography.labelMedium,
                    color = RoxoNeonClaro
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                Text(
                    text = manga.nome,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextoPrincipal
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                Row {
                    BadgeInfo(texto = manga.categoria)
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    BadgeInfo(texto = "Vol. ${manga.volume}")
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                PriceText(preco = manga.preco)

                if (esgotado) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "Produto esgotado no momento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (estoqueBaixo) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "Últimas ${manga.estoque} unidades!",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmareloDestaque
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // ---------- Descrição ----------
            SectionHeader(titulo = "Descrição")
            Text(
                text = manga.descricao.ifBlank { "Sem descrição disponível." },
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario,
                modifier = Modifier.padding(horizontal = Spacing.lg)
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // ---------- Informações ----------
            SectionHeader(titulo = "Informações")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .clip(RoundedCornerShape(Spacing.radiusMedium))
                    .background(FundoCard)
                    .padding(Spacing.md)
            ) {
                LinhaInfo(rotulo = "Editora", valor = manga.marca.ifBlank { "—" })
                LinhaInfo(rotulo = "Categoria", valor = manga.categoria.ifBlank { "—" })
                LinhaInfo(rotulo = "Volume", valor = manga.volume.toString())
                LinhaInfo(
                    rotulo = "Estoque",
                    valor = if (esgotado) "Esgotado" else "${manga.estoque} unidades",
                    ultima = true
                )
            }

            // ---------- Recomendações ----------
            if (recomendados.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.lg))
                SectionHeader(titulo = "Você também pode gostar")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(recomendados, key = { it.id }) { recomendado ->
                        MangaCardFavoritavel(
                            manga = recomendado,
                            isFavorito = recomendado.id in favoritosIds,
                            onClick = { onMangaClick(recomendado) },
                            onFavoritoClick = { favoritoViewModel.alternarFavorito(usuarioId, recomendado) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }

        // ---------- Botão fixo ----------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(Spacing.lg)
        ) {
            PrimaryButton(
                text = if (esgotado) "Produto esgotado" else "Adicionar ao Carrinho",
                onClick = { onAdicionarAoCarrinho(manga) },
                enabled = !esgotado,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Botão circular flutuante usado sobre a capa (voltar, favoritar).
 * Local a esta tela pois seu estilo (fundo translúcido sobre imagem) é específico
 * do header da capa, diferente do IconButton padrão usado no resto do app.
 */
@Composable
private fun BotaoCircular(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = TextoPrincipal,
    escala: Float = 1f
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icone,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.scale(escala)
        )
    }
}

/** Selo pequeno para categoria/volume, apenas leitura (sem estado de seleção). */
@Composable
private fun BadgeInfo(texto: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Spacing.radiusPill))
            .background(FundoCard)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
    ) {
        Text(text = texto, style = MaterialTheme.typography.labelSmall, color = TextoSecundario)
    }
}

/** Linha "rótulo — valor" usada no card de Informações. */
@Composable
private fun LinhaInfo(rotulo: String, valor: String, ultima: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = rotulo, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
        Text(text = valor, style = MaterialTheme.typography.bodyMedium, color = TextoPrincipal)
    }
    if (!ultima) {
        HorizontalDivider(color = TextoSecundario.copy(alpha = 0.15f))
    }
}