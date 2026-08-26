package com.example.zeromangas.ui.theme.enderecos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.zeromangas.data.model.Endereco
import com.example.zeromangas.ui.components.PrimaryButton
import com.example.zeromangas.ui.components.EmptyState
import com.example.zeromangas.ui.components.LoadingState
import com.example.zeromangas.ui.components.SecondaryButton
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.RoxoNeonClaro
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.VermelhoErro
import com.example.zeromangas.viewmodel.EnderecoViewModel
import com.example.zeromangas.viewmodel.EnderecosState

/**
 * Tela "Meus Endereços": lista os endereços do cliente (padrão em destaque),
 * com adicionar/editar/excluir/definir padrão via [EnderecoViewModel]. O
 * formulário (com CEP automático via ViaCEP) fica num bottom sheet, reaberto
 * tanto para criar quanto para editar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnderecosScreen(
    enderecoViewModel: EnderecoViewModel,
    onVoltar: () -> Unit
) {
    val enderecosState by enderecoViewModel.enderecosState.collectAsState()

    var mostrarFormulario by remember { mutableStateOf(false) }
    var enderecoEmEdicao by remember { mutableStateOf<Endereco?>(null) }
    var enderecoParaExcluir by remember { mutableStateOf<Endereco?>(null) }

    LaunchedEffect(Unit) {
        enderecoViewModel.carregarEnderecos()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoltar) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextoPrincipal)
            }
            Text(
                text = "Meus Endereços",
                style = MaterialTheme.typography.titleLarge,
                color = TextoPrincipal,
                modifier = Modifier
                    .padding(start = Spacing.sm)
                    .weight(1f)
            )
            IconButton(onClick = {
                enderecoEmEdicao = null
                mostrarFormulario = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar endereço", tint = RoxoNeon)
            }
        }

        when (val estado = enderecosState) {
            is EnderecosState.Carregando -> LoadingState(modifier = Modifier.weight(1f))

            is EnderecosState.Erro -> EmptyState(
                modifier = Modifier.weight(1f),
                titulo = "Não foi possível carregar",
                subtitulo = estado.mensagem,
                icone = Icons.Outlined.LocationOff,
                textoAcao = "Tentar novamente",
                onAcaoClick = { enderecoViewModel.carregarEnderecos() }
            )

            is EnderecosState.Sucesso -> {
                if (estado.enderecos.isEmpty()) {
                    EmptyState(
                        modifier = Modifier.weight(1f),
                        titulo = "Nenhum endereço cadastrado",
                        subtitulo = "Adicione um endereço para agilizar suas compras.",
                        icone = Icons.Outlined.LocationOff,
                        textoAcao = "Adicionar endereço",
                        onAcaoClick = {
                            enderecoEmEdicao = null
                            mostrarFormulario = true
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        items(estado.enderecos, key = { it.id }) { endereco ->
                            CardEndereco(
                                endereco = endereco,
                                onEditar = {
                                    enderecoEmEdicao = endereco
                                    mostrarFormulario = true
                                },
                                onExcluir = { enderecoParaExcluir = endereco },
                                onDefinirPadrao = { enderecoViewModel.definirComoPadrao(endereco.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(Spacing.lg)) }
                    }
                }
            }
        }
    }

    if (mostrarFormulario) {
        FormularioEnderecoBottomSheet(
            enderecoViewModel = enderecoViewModel,
            enderecoParaEditar = enderecoEmEdicao,
            onFechar = { mostrarFormulario = false }
        )
    }

    enderecoParaExcluir?.let { endereco ->
        AlertDialog(
            onDismissRequest = { enderecoParaExcluir = null },
            title = { Text("Excluir endereço") },
            text = { Text("Tem certeza que deseja excluir o endereço de ${endereco.nomeDestinatario.ifBlank { "este destinatário" }}?") },
            confirmButton = {
                TextButton(onClick = {
                    enderecoViewModel.excluirEndereco(endereco.id)
                    enderecoParaExcluir = null
                }) {
                    Text("Excluir", color = VermelhoErro)
                }
            },
            dismissButton = {
                TextButton(onClick = { enderecoParaExcluir = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun CardEndereco(
    endereco: Endereco,
    onEditar: () -> Unit,
    onExcluir: () -> Unit,
    onDefinirPadrao: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Spacing.radiusMedium))
            .background(FundoCard)
            .padding(Spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (endereco.padrao) RoxoNeonClaro else TextoSecundario,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(
                text = endereco.nomeDestinatario.ifBlank { "Destinatário não informado" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextoPrincipal,
                modifier = Modifier.weight(1f)
            )
            if (endereco.padrao) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Spacing.radiusPill))
                        .background(RoxoNeon.copy(alpha = 0.15f))
                        .padding(horizontal = Spacing.sm, vertical = 2.dp)
                ) {
                    Text("Padrão", style = MaterialTheme.typography.labelSmall, color = RoxoNeonClaro)
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = "${endereco.logradouro}, ${endereco.numero}" +
                    if (endereco.complemento.isNotBlank()) " - ${endereco.complemento}" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario
        )
        Text(
            text = "${endereco.bairro} - ${endereco.cidade}/${endereco.uf}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario
        )
        Text(
            text = "CEP ${endereco.cep}",
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario
        )
        if (endereco.telefone.isNotBlank()) {
            Text(
                text = endereco.telefone,
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            TextButton(onClick = onEditar) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoxoNeon)
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text("Editar", color = RoxoNeon)
            }
            TextButton(onClick = onExcluir) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = VermelhoErro)
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text("Excluir", color = VermelhoErro)
            }
            if (!endereco.padrao) {
                TextButton(onClick = onDefinirPadrao) {
                    Text("Tornar padrão", color = TextoSecundario)
                }
            }
        }
    }
}