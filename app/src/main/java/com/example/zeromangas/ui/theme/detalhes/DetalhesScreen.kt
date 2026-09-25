package com.example.zeromangas.ui.theme.detalhes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.zeromangas.ui.components.RatingStars
import com.example.zeromangas.ui.components.SectionHeader
import com.example.zeromangas.ui.theme.AmareloDestaque
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.RoxoNeonClaro
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.viewmodel.FavoritoViewModel
import com.example.zeromangas.viewmodel.AvaliacaoViewModel
import com.example.zeromangas.viewmodel.AvaliacoesState

/**
 * Tela de detalhes de um mangá.
 *
 * Reaproveita os componentes do design system (PriceText, PrimaryButton, SectionHeader,
 * MangaCardFavoritavel) já usados na Home/Busca/Favoritos, em vez de recriar visual novo.
 *
 * A tela consome somente informações existentes em [Manga] e as avaliações já
 * persistidas, mantendo conteúdo e regras de negócio fora da camada visual.
 */
@Composable
fun DetalhesScreen(
    manga: Manga?,
    favoritoViewModel: FavoritoViewModel,
    usuarioId: String,
    onVoltar: () -> Unit,
    onAdicionarAoCarrinho: (Manga, Int) -> Unit,
    recomendados: List<Manga> = emptyList(),
    onMangaClick: (Manga) -> Unit = {},
    onRequerLogin: () -> Unit = {},
    avaliacaoViewModel: AvaliacaoViewModel
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
    // Favoritos são vinculados à conta (item 7): visitante é direcionado pro
    // fluxo de login/cadastro em vez de a chamada falhar em silêncio.
    val aoFavoritar: (Manga) -> Unit = { alvo ->
        if (usuarioId.isBlank()) onRequerLogin() else favoritoViewModel.alternarFavorito(usuarioId, alvo)
    }
    val avaliacoesState by avaliacaoViewModel.avaliacoesState.collectAsState()

    // ETAPA 11 (polimento): mesmo "pulo" do coração usado no MangaCardFavoritavel,
    // aqui aplicado ao botão de favorito grande sobre a capa.
    val escalaFavorito by animateFloatAsState(
        targetValue = if (isFavorito) 1.08f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "escalaFavoritoDetalhes"
    )

    LaunchedEffect(usuarioId) {
        favoritoViewModel.carregarFavoritos(usuarioId)
    }
    LaunchedEffect(manga.id) { avaliacaoViewModel.carregarAvaliacoes(manga.id) }

    val esgotado = manga.estoque <= 0
    val estoqueBaixo = manga.estoque in 1..5

    // ETAPA 5 (Detalhes): quantidade escolhida antes de adicionar ao carrinho.
    // Fica limitada ao estoque disponível pra não deixar o usuário pedir mais
    // do que existe (o CartViewModel também valida isso, mas evitamos o aviso
    // de estoque aparecendo sem necessidade).
    var quantidade by remember(manga.id) { mutableIntStateOf(1) }

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
                    .height(Spacing.heroCoverHeight)
                    .clip(RoundedCornerShape(bottomStart = Spacing.radiusLarge, bottomEnd = Spacing.radiusLarge))
                    .background(FundoCard)
            ) {
                AsyncImage(
                    model = manga.imagemUrl,
                    contentDescription = manga.nome,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
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
                        onClick = { aoFavoritar(manga) }
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

                if (manga.totalAvaliacoes > 0) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    RatingStars(nota = manga.notaMedia)
                    Text(
                        text = "${manga.totalAvaliacoes} ${if (manga.totalAvaliacoes == 1) "avaliação" else "avaliações"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        modifier = Modifier.padding(top = Spacing.xs)
                    )
                }

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

                val precoAtual = manga.precoPromocional?.takeIf { manga.emPromocao && it < manga.preco } ?: manga.preco
                PriceText(preco = precoAtual, precoAntigo = manga.preco.takeIf { precoAtual < it })

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
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg)) {
                if (manga.autor.isNotBlank()) LinhaInfo(rotulo = "Autor", valor = manga.autor)
                LinhaInfo(rotulo = "Editora", valor = manga.marca.ifBlank { "—" })
                LinhaInfo(rotulo = "Gênero", valor = manga.categoria.ifBlank { "—" })
                LinhaInfo(rotulo = "Volume", valor = manga.volume.toString())
                LinhaInfo(
                    rotulo = "Estoque",
                    valor = if (esgotado) "Esgotado" else "${manga.estoque} unidades",
                    ultima = true
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
            SectionHeader(titulo = "Avaliações")
            AvaliacoesConteudo(estado = avaliacoesState, manga = manga)

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
                            onFavoritoClick = { aoFavoritar(recomendado) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }

        // ---------- Quantidade + Botão fixo ----------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
        ) {
            if (!esgotado) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quantidade",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                    QuantidadeSelector(
                        quantidade = quantidade,
                        podeAumentar = quantidade < manga.estoque,
                        onDiminuir = { if (quantidade > 1) quantidade-- },
                        onAumentar = { if (quantidade < manga.estoque) quantidade++ }
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            PrimaryButton(
                text = if (esgotado) "Produto esgotado" else "Adicionar ao Carrinho",
                onClick = { onAdicionarAoCarrinho(manga, quantidade) },
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

/**
 * Seletor "− quantidade +" usado antes do botão de adicionar ao carrinho.
 * Local a esta tela por enquanto — se precisar em outro lugar (ex.: Carrinho
 * já tem o seu próprio, em CartScreen), extrair para ui/components.
 */
@Composable
private fun QuantidadeSelector(
    quantidade: Int,
    podeAumentar: Boolean,
    onDiminuir: () -> Unit,
    onAumentar: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Spacing.radiusPill))
            .background(FundoCard),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BotaoQuantidade(
            icone = Icons.Default.Remove,
            contentDescription = "Diminuir quantidade",
            habilitado = quantidade > 1,
            onClick = onDiminuir
        )
        Text(
            text = quantidade.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = TextoPrincipal,
            modifier = Modifier.padding(horizontal = Spacing.md)
        )
        BotaoQuantidade(
            icone = Icons.Default.Add,
            contentDescription = "Aumentar quantidade",
            habilitado = podeAumentar,
            onClick = onAumentar
        )
    }
}

@Composable
private fun BotaoQuantidade(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    habilitado: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(enabled = habilitado, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icone,
            contentDescription = contentDescription,
            tint = if (habilitado) RoxoNeonClaro else TextoSecundario.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
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

@Composable
private fun AvaliacoesConteudo(estado: AvaliacoesState?, manga: Manga) {
    when (estado) {
        AvaliacoesState.Carregando -> LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
            color = MaterialTheme.colorScheme.primary
        )
        is AvaliacoesState.Sucesso -> {
            val avaliacoes = estado.avaliacoes
            if (avaliacoes.isEmpty()) {
                Text(
                    text = if (manga.totalAvaliacoes > 0) "As avaliações ainda não estão disponíveis." else "Ainda não há avaliações para este mangá.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario,
                    modifier = Modifier.padding(horizontal = Spacing.lg)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    avaliacoes.forEach { avaliacao ->
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = avaliacao.nomeCliente.ifBlank { "Cliente" },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextoPrincipal
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                RatingStars(nota = avaliacao.nota.toDouble(), mostrarValor = false)
                            }
                            if (avaliacao.comentario.isNotBlank()) {
                                Text(avaliacao.comentario, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
                            }
                        }
                    }
                }
            }
        }
        is AvaliacoesState.Erro -> Text(
            text = "Não foi possível carregar as avaliações.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario,
            modifier = Modifier.padding(horizontal = Spacing.lg)
        )
        null -> Unit
    }
}