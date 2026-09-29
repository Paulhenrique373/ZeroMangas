package com.example.zeromangas.ui.theme.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.zeromangas.repository.CupomAdminDto
import com.example.zeromangas.ui.components.EmptyState
import com.example.zeromangas.ui.components.LoadingState
import com.example.zeromangas.ui.components.formatarPrecoBr
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.RoxoNeonClaro
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.VerdeSucesso
import com.example.zeromangas.ui.theme.VermelhoErro
import com.example.zeromangas.viewmodel.AdminCuponsViewModel
import kotlinx.coroutines.launch

private fun descricaoDesconto(c: CupomAdminDto): String =
    if (c.tipoDesconto.uppercase() == "FIXO") {
        "${formatarPrecoBr(c.valor)} de desconto"
    } else {
        val texto = if (c.valor % 1.0 == 0.0) c.valor.toInt().toString() else c.valor.toString()
        "$texto% de desconto"
    }

private fun textoUsos(c: CupomAdminDto): String =
    if (c.limiteTotal > 0) "${c.usos} de ${c.limiteTotal} usos" else "${c.usos} usos (sem limite)"

/** Numa caixa de texto o admin pode digitar "10,50" ou "10.50". */
private fun paraDecimal(texto: String): Double? =
    texto.trim().replace(',', '.').toDoubleOrNull()

/**
 * Cupons do Painel Administrativo: listar, criar, editar e ativar/desativar.
 * Toda escrita passa por RPC com sou_admin() no banco.
 */
@Composable
fun AdminCuponsScreen(viewModel: AdminCuponsViewModel, onVoltar: () -> Unit) {
    val cupons by viewModel.cupons.collectAsState()
    val carregando by viewModel.carregando.collectAsState()
    val erro by viewModel.erro.collectAsState()

    val escopo = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var formAberto by remember { mutableStateOf(false) }
    var cupomEmEdicao by remember { mutableStateOf<CupomAdminDto?>(null) }
    var cupomParaAlternar by remember { mutableStateOf<CupomAdminDto?>(null) }

    LaunchedEffect(Unit) { viewModel.carregar() }

    fun avisar(mensagem: String) {
        escopo.launch { snackbarHostState.showSnackbar(mensagem) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.screenHorizontal),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onVoltar) {
                    Icon(Icons.Default.ArrowBack, "Voltar", tint = TextoPrincipal)
                }
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    "Cupons",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextoPrincipal,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(onClick = {
                    cupomEmEdicao = null
                    formAberto = true
                }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(Spacing.xs))
                    Text("Novo")
                }
            }

            when {
                carregando && cupons.isEmpty() -> LoadingState(modifier = Modifier.weight(1f))
                erro != null && cupons.isEmpty() -> EmptyState(
                    titulo = "Não foi possível carregar",
                    subtitulo = erro,
                    textoAcao = "Tentar novamente",
                    onAcaoClick = { viewModel.carregar() },
                    modifier = Modifier.weight(1f)
                )
                cupons.isEmpty() -> EmptyState(
                    titulo = "Nenhum cupom cadastrado.",
                    subtitulo = "Toque em \"Novo\" para criar o primeiro.",
                    modifier = Modifier.weight(1f)
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(Spacing.screenHorizontal),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.weight(1f)
                ) {
                    items(cupons, key = { it.id }) { cupom ->
                        CupomItem(
                            cupom = cupom,
                            onClick = {
                                cupomEmEdicao = cupom
                                formAberto = true
                            },
                            onAlternar = { cupomParaAlternar = cupom }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
        )
    }

    if (formAberto) {
        val editando = cupomEmEdicao
        FormularioCupomDialog(
            cupomExistente = editando,
            onFechar = { formAberto = false },
            onSalvar = { codigo, tipo, valor, minimo, limite ->
                if (editando == null) {
                    viewModel.criar(codigo, tipo, valor, minimo, limite)
                        .onSuccess { avisar("Cupom criado com sucesso.") }
                } else {
                    viewModel.atualizar(editando.id, tipo, valor, minimo, limite)
                        .onSuccess { avisar("Cupom atualizado com sucesso.") }
                }
            }
        )
    }

    cupomParaAlternar?.let { cupom ->
        val desativando = cupom.ativo
        AlertDialog(
            onDismissRequest = { cupomParaAlternar = null },
            title = { Text(if (desativando) "Desativar cupom?" else "Ativar cupom?") },
            text = {
                Text(
                    if (desativando) "Deseja desativar este cupom? Ele deixa de valer no checkout."
                    else "Deseja ativar este cupom? Ele volta a valer no checkout."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    cupomParaAlternar = null
                    escopo.launch {
                        viewModel.alternarAtivo(cupom.id, !cupom.ativo)
                            .onSuccess {
                                avisar(if (desativando) "Cupom desativado." else "Cupom ativado.")
                            }
                            .onFailure { avisar(it.message ?: "Não foi possível alterar o cupom.") }
                    }
                }) {
                    Text(
                        if (desativando) "Desativar" else "Ativar",
                        color = if (desativando) VermelhoErro else RoxoNeonClaro
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { cupomParaAlternar = null }) { Text("Voltar") }
            }
        )
    }
}

@Composable
private fun CupomItem(cupom: CupomAdminDto, onClick: () -> Unit, onAlternar: () -> Unit) {
    Surface(
        color = FundoCard,
        shape = MaterialTheme.shapes.large,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(cupom.codigo, style = MaterialTheme.typography.titleMedium, color = TextoPrincipal)
                Text(descricaoDesconto(cupom), style = MaterialTheme.typography.bodyMedium, color = RoxoNeonClaro)
                if (cupom.valorMinimo > 0) {
                    Text(
                        "Compra mínima: ${formatarPrecoBr(cupom.valorMinimo)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                }
                Text(textoUsos(cupom), style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                Text(
                    if (cupom.ativo) "Ativo" else "Inativo",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (cupom.ativo) VerdeSucesso else VermelhoErro
                )
            }
            Switch(
                checked = cupom.ativo,
                onCheckedChange = { onAlternar() },
                colors = SwitchDefaults.colors(checkedTrackColor = RoxoNeon)
            )
        }
    }
}

@Composable
private fun FormularioCupomDialog(
    cupomExistente: CupomAdminDto?,
    onFechar: () -> Unit,
    onSalvar: suspend (codigo: String, tipo: String, valor: Double, valorMinimo: Double, limite: Int) -> Result<Unit>
) {
    val escopo = rememberCoroutineScope()
    val editando = cupomExistente != null

    var codigo by remember { mutableStateOf(cupomExistente?.codigo ?: "") }
    var tipo by remember { mutableStateOf(cupomExistente?.tipoDesconto?.uppercase() ?: "PERCENTUAL") }
    var valorTxt by remember { mutableStateOf(cupomExistente?.valor?.toString() ?: "") }
    var minimoTxt by remember {
        mutableStateOf(cupomExistente?.valorMinimo?.takeIf { it > 0 }?.toString() ?: "")
    }
    var limiteTxt by remember {
        mutableStateOf(cupomExistente?.limiteTotal?.takeIf { it > 0 }?.toString() ?: "")
    }
    var salvando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!salvando) onFechar() },
        title = { Text(if (editando) "Editar cupom" else "Novo cupom") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it.uppercase() },
                    label = { Text("Código") },
                    supportingText = {
                        Text(if (editando) "O código não pode ser alterado." else "Letras, números e hífen.")
                    },
                    enabled = !editando,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    FilterChip(
                        selected = tipo == "PERCENTUAL",
                        onClick = { tipo = "PERCENTUAL" },
                        label = { Text("Percentual (%)") }
                    )
                    FilterChip(
                        selected = tipo == "FIXO",
                        onClick = { tipo = "FIXO" },
                        label = { Text("Valor fixo (R$)") }
                    )
                }

                OutlinedTextField(
                    value = valorTxt,
                    onValueChange = { valorTxt = it },
                    label = { Text(if (tipo == "FIXO") "Desconto (R$)" else "Desconto (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = minimoTxt,
                    onValueChange = { minimoTxt = it },
                    label = { Text("Compra mínima (R$)") },
                    supportingText = { Text("Vazio = sem mínimo.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = limiteTxt,
                    onValueChange = { limiteTxt = it.filter(Char::isDigit) },
                    label = { Text("Limite de usos") },
                    supportingText = { Text("Vazio = sem limite.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (erro != null) {
                    Text(erro!!, style = MaterialTheme.typography.bodySmall, color = VermelhoErro)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !salvando,
                onClick = {
                    val valor = paraDecimal(valorTxt)
                    val minimo = if (minimoTxt.isBlank()) 0.0 else paraDecimal(minimoTxt)
                    val limite = if (limiteTxt.isBlank()) 0 else limiteTxt.toIntOrNull()

                    when {
                        codigo.isBlank() -> erro = "Informe o código do cupom."
                        valor == null || valor <= 0 -> erro = "Informe um desconto maior que zero."
                        minimo == null || minimo < 0 -> erro = "Compra mínima inválida."
                        limite == null -> erro = "Limite de usos inválido."
                        else -> escopo.launch {
                            salvando = true
                            erro = null
                            onSalvar(codigo.trim(), tipo, valor, minimo, limite)
                                .onSuccess { onFechar() }
                                .onFailure { erro = it.message ?: "Não foi possível salvar o cupom." }
                            salvando = false
                        }
                    }
                }
            ) { Text("Salvar", color = RoxoNeonClaro) }
        },
        dismissButton = {
            TextButton(enabled = !salvando, onClick = onFechar) { Text("Cancelar") }
        }
    )
}