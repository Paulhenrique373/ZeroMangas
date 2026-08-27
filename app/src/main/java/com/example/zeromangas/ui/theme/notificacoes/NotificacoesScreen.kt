package com.example.zeromangas.ui.theme.notificacoes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.zeromangas.data.model.Notificacao
import com.example.zeromangas.data.model.TipoNotificacao
import com.example.zeromangas.ui.components.EmptyState
import com.example.zeromangas.ui.components.LoadingState
import com.example.zeromangas.ui.theme.AmareloDestaque
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.FundoCardClaro
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.RoxoNeonClaro
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.VerdeSucesso
import com.example.zeromangas.viewmodel.NotificacaoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Cor semântica de cada tipo de notificação — mesmo espírito de [com.example.zeromangas.ui.components.corDoStatusPedido],
 * mas para o domínio de notificações.
 */
private fun corDoTipo(tipo: TipoNotificacao): androidx.compose.ui.graphics.Color = when (tipo) {
    TipoNotificacao.PROMOCAO -> AmareloDestaque
    TipoNotificacao.LANCAMENTO -> RoxoNeonClaro
    TipoNotificacao.PEDIDO -> RoxoNeon
    TipoNotificacao.FAVORITO_PROMOCAO -> AmareloDestaque
    TipoNotificacao.FAVORITO_ESTOQUE -> VerdeSucesso
}

private fun iconeDoTipo(tipo: TipoNotificacao): ImageVector = when (tipo) {
    TipoNotificacao.PROMOCAO -> Icons.Filled.LocalOffer
    TipoNotificacao.LANCAMENTO -> Icons.Filled.NewReleases
    TipoNotificacao.PEDIDO -> Icons.Filled.Receipt
    TipoNotificacao.FAVORITO_PROMOCAO -> Icons.Outlined.Favorite
    TipoNotificacao.FAVORITO_ESTOQUE -> Icons.Outlined.Inventory2
}

/**
 * "há 5 min" / "há 3h" / "ontem" / "12/08" — igual ao espírito de apps de loja
 * (Shopee, Amazon). Cai pra data completa quando passa de uma semana.
 * O parsing de ISO reaproveita a mesma tolerância a formatos de
 * [com.example.zeromangas.repository.OrderRepository] (com ou sem milissegundos/offset).
 */
private fun tempoRelativo(iso: String, agora: Long): String {
    if (iso.isBlank()) return ""
    val millis = try {
        val semOffset = iso.replace(Regex("([+-]\\d{2}:?\\d{2}|Z)$"), "")
        val ajustado = semOffset.replace(Regex("\\.(\\d{3})\\d*$"), ".$1")
        val padrao = if (ajustado.contains(".")) "yyyy-MM-dd'T'HH:mm:ss.SSS" else "yyyy-MM-dd'T'HH:mm:ss"
        val sdf = SimpleDateFormat(padrao, Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        sdf.parse(ajustado)?.time ?: return ""
    } catch (e: Exception) {
        return ""
    }

    val diffMs = (agora - millis).coerceAtLeast(0)
    val minutos = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val horas = TimeUnit.MILLISECONDS.toHours(diffMs)
    val dias = TimeUnit.MILLISECONDS.toDays(diffMs)

    return when {
        minutos < 1 -> "agora"
        minutos < 60 -> "há $minutos min"
        horas < 24 -> "há ${horas}h"
        dias == 1L -> "ontem"
        dias < 7 -> "há $dias dias"
        else -> SimpleDateFormat("dd/MM", Locale("pt", "BR")).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(millis))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacoesScreen(
    notificacaoViewModel: NotificacaoViewModel,
    usuarioId: String,
    onVoltar: () -> Unit,
    onNotificacaoClick: (Notificacao) -> Unit = {}
) {
    val notificacoes by notificacaoViewModel.notificacoes.collectAsState()
    val quantidadeNaoLidas by notificacaoViewModel.quantidadeNaoLidas.collectAsState()

    var carregando by remember { mutableStateOf(true) }

    // "Relógio" simples só pra recalcular o tempo relativo ("há X min") periodicamente,
    // no mesmo espírito do ticker de status usado em PedidosScreen.
    var agora by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            agora = System.currentTimeMillis()
        }
    }

    LaunchedEffect(usuarioId) {
        notificacaoViewModel.iniciar(usuarioId)
        carregando = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ---- Topo: voltar + título + "marcar todas como lidas" ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVoltar) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = TextoPrincipal
                    )
                }
                Text(
                    text = "Notificações",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextoPrincipal,
                    fontWeight = FontWeight.Bold
                )
            }

            if (quantidadeNaoLidas > 0) {
                TextButton(onClick = { notificacaoViewModel.marcarTodasComoLidas() }) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        tint = RoxoNeonClaro,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Marcar todas", color = RoxoNeonClaro, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        when {
            carregando -> LoadingState(modifier = Modifier.fillMaxSize())

            notificacoes.isEmpty() -> EmptyState(
                titulo = "Nenhuma notificação por aqui",
                subtitulo = "Promoções, lançamentos e atualizações dos seus pedidos aparecem nesta tela.",
                icone = Icons.Outlined.NotificationsNone,
                modifier = Modifier.fillMaxSize()
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(notificacoes, key = { it.id }) { notificacao ->
                    ItemNotificacao(
                        notificacao = notificacao,
                        tempoTexto = tempoRelativo(notificacao.criadoEm, agora),
                        onClick = {
                            if (!notificacao.lida) {
                                notificacaoViewModel.marcarComoLida(notificacao.id)
                            }
                            onNotificacaoClick(notificacao)
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(Spacing.md)) }
            }
        }
    }
}

@Composable
private fun ItemNotificacao(
    notificacao: Notificacao,
    tempoTexto: String,
    onClick: () -> Unit
) {
    val cor = corDoTipo(notificacao.tipo)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Spacing.radiusMedium))
            .background(if (notificacao.lida) FundoCard else FundoCardClaro)
            .then(
                if (!notificacao.lida) {
                    Modifier.background(
                        RoxoNeon.copy(alpha = 0.06f),
                        RoundedCornerShape(Spacing.radiusMedium)
                    )
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(Spacing.md),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(cor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconeDoTipo(notificacao.tipo),
                contentDescription = null,
                tint = cor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = notificacao.titulo,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextoPrincipal,
                    fontWeight = if (notificacao.lida) FontWeight.Normal else FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (tempoTexto.isNotBlank()) {
                    Text(
                        text = tempoTexto,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoSecundario
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = notificacao.mensagem,
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
        }

        if (!notificacao.lida) {
            Spacer(modifier = Modifier.width(Spacing.xs))
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(RoxoNeon)
            )
        }
    }
}