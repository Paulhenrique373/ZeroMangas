package com.example.zeromangas.ui.theme.editarperfil

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zeromangas.ui.components.PrimaryButton
import com.example.zeromangas.ui.components.SecondaryButton
import com.example.zeromangas.ui.theme.BordaSutil
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.VerdeSucesso
import com.example.zeromangas.ui.theme.VermelhoErro
import com.example.zeromangas.viewmodel.AuthViewModel
import com.example.zeromangas.viewmodel.CredenciaisState
import com.example.zeromangas.viewmodel.PerfilCompletoState
import com.example.zeromangas.viewmodel.UploadFotoState
import java.time.DateTimeException
import java.time.LocalDate

private val OPCOES_GENERO = listOf("Prefiro não informar", "Feminino", "Masculino", "Não-binário", "Outro")

/**
 * Tela completa de edição de perfil. Reaproveita o [AuthViewModel] já existente
 * (upload de foto, atualização de nome — agora persistido no Supabase também) e
 * adiciona os campos novos (telefone, CPF, bio, gênero, nascimento) + troca de
 * e-mail e senha via Supabase Auth. Não mexe em nenhuma lógica de pedidos/carrinho.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPerfilScreen(
    authViewModel: AuthViewModel,
    onVoltar: () -> Unit
) {
    val context = LocalContext.current
    val usuario by authViewModel.usuarioAtual.collectAsState()
    val perfilCliente by authViewModel.perfilCliente.collectAsState()
    val perfilCompletoState by authViewModel.perfilCompletoState.collectAsState()
    val uploadFotoState by authViewModel.uploadFotoState.collectAsState()
    val credenciaisState by authViewModel.credenciaisState.collectAsState()

    var nome by remember { mutableStateOf("") }
    var fotoUrl by remember { mutableStateOf("") }
    var fotoLocalPreview by remember { mutableStateOf<Uri?>(null) }
    // Celular, CPF e data guardam SÓ os dígitos; a máscara (parênteses, pontos,
    // barras) é aplicada visualmente por MascaraTransformation. Assim o cursor
    // não pula e apagar/editar no meio do texto funciona normal.
    var telefone by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var genero by remember { mutableStateOf("") }
    var dataNascimento by remember { mutableStateOf("") }
    var jaCarregouCampos by remember { mutableStateOf(false) }
    var tentouSalvar by remember { mutableStateOf(false) }
    var mostrarSeletorGenero by remember { mutableStateOf(false) }
    var mostrarDialogoEmail by remember { mutableStateOf(false) }
    var mostrarDialogoSenha by remember { mutableStateOf(false) }

    val seletorImagem = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            fotoLocalPreview = uri
            authViewModel.uploadFotoPerfil(context, uri)
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.carregarUsuario()
        authViewModel.carregarPerfilCompleto()
    }

    LaunchedEffect(usuario, perfilCliente) {
        if (!jaCarregouCampos && usuario != null) {
            nome = usuario?.nome.orEmpty()
            fotoUrl = perfilCliente?.fotoUrl?.ifBlank { usuario?.fotoUrl.orEmpty() } ?: usuario?.fotoUrl.orEmpty()
            telefone = perfilCliente?.telefone.orEmpty().somenteDigitos().take(11)
            cpf = perfilCliente?.cpf.orEmpty().somenteDigitos().take(11)
            bio = perfilCliente?.bio.orEmpty()
            genero = perfilCliente?.genero.orEmpty()
            dataNascimento = isoParaDataBr(perfilCliente?.dataNascimento.orEmpty()).somenteDigitos().take(8)
            jaCarregouCampos = true
        }
    }

    LaunchedEffect(uploadFotoState) {
        val estado = uploadFotoState
        if (estado is UploadFotoState.Sucesso) {
            fotoUrl = estado.url
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.resetarPerfilCompletoState()
        authViewModel.resetarCredenciaisState()
    }

    val msgErroTelefone = erroTelefone(telefone, tentouSalvar)
    val msgErroCpf = erroCpf(cpf, tentouSalvar)
    val msgErroData = erroData(dataNascimento, tentouSalvar)
    val coresCampo = coresDoCampo()
    val formaCampo = MaterialTheme.shapes.small

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
                text = "Editar Perfil",
                style = MaterialTheme.typography.titleLarge,
                color = TextoPrincipal,
                modifier = Modifier.padding(start = Spacing.sm)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
        ) {
            // ---------- Foto ----------
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(FundoCard)
                        .clickable { seletorImagem.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val modeloImagem = fotoLocalPreview ?: fotoUrl.ifBlank { null }

                    if (modeloImagem != null) {
                        AsyncImage(
                            model = modeloImagem,
                            contentDescription = "Foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.size(100.dp),
                            tint = TextoSecundario
                        )
                    }

                    if (uploadFotoState is UploadFotoState.Loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = TextoPrincipal)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(RoxoNeon),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Trocar foto",
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Toque na foto para escolher uma da galeria",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            SecaoCard(titulo = "Dados pessoais") {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome completo") },
                    singleLine = true,
                    isError = tentouSalvar && nome.isBlank(),
                    supportingText = if (tentouSalvar && nome.isBlank()) { { Text("O nome não pode ficar em branco") } } else null,
                    shape = formaCampo,
                    colors = coresCampo,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Spacing.sm))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 160) bio = it },
                    label = { Text("Bio") },
                    supportingText = { Text("${bio.length}/160") },
                    minLines = 2,
                    maxLines = 4,
                    shape = formaCampo,
                    colors = coresCampo,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Spacing.sm))

                Box {
                    OutlinedTextField(
                        value = genero.ifBlank { "" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gênero (opcional)") },
                        trailingIcon = { Icon(Icons.Default.ExpandMore, contentDescription = null) },
                        shape = formaCampo,
                        colors = coresCampo,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Camada transparente por cima pra capturar o clique
                    // (OutlinedTextField readOnly ainda intercepta o toque).
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { mostrarSeletorGenero = true }
                    )
                    DropdownMenu(
                        expanded = mostrarSeletorGenero,
                        onDismissRequest = { mostrarSeletorGenero = false }
                    ) {
                        OPCOES_GENERO.forEach { opcao ->
                            DropdownMenuItem(
                                text = { Text(opcao) },
                                onClick = {
                                    genero = opcao
                                    mostrarSeletorGenero = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.sm))

                OutlinedTextField(
                    value = dataNascimento,
                    onValueChange = { dataNascimento = it.somenteDigitos().take(8) },
                    label = { Text("Data de nascimento") },
                    placeholder = { Text("DD/MM/AAAA") },
                    singleLine = true,
                    isError = msgErroData != null,
                    supportingText = msgErroData?.let { msg -> { Text(msg) } },
                    visualTransformation = MascaraTransformation(::formatarData),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = formaCampo,
                    colors = coresCampo,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            SecaoCard(titulo = "Contato e documento") {
                OutlinedTextField(
                    value = telefone,
                    onValueChange = { telefone = it.somenteDigitos().take(11) },
                    label = { Text("Celular") },
                    placeholder = { Text("(11) 99999-9999") },
                    singleLine = true,
                    isError = msgErroTelefone != null,
                    supportingText = msgErroTelefone?.let { msg -> { Text(msg) } },
                    visualTransformation = MascaraTransformation(::formatarTelefone),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = formaCampo,
                    colors = coresCampo,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Spacing.sm))

                OutlinedTextField(
                    value = cpf,
                    onValueChange = { cpf = it.somenteDigitos().take(11) },
                    label = { Text("CPF") },
                    placeholder = { Text("000.000.000-00") },
                    singleLine = true,
                    isError = msgErroCpf != null,
                    supportingText = msgErroCpf?.let { msg -> { Text(msg) } },
                    visualTransformation = MascaraTransformation(::formatarCpf),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = formaCampo,
                    colors = coresCampo,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when (val estado = perfilCompletoState) {
                is PerfilCompletoState.Erro -> {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(estado.mensagem, style = MaterialTheme.typography.bodySmall, color = VermelhoErro)
                }
                is PerfilCompletoState.Sucesso -> {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text("Perfil atualizado com sucesso!", style = MaterialTheme.typography.bodySmall, color = VerdeSucesso)
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            PrimaryButton(
                text = "Salvar alterações",
                onClick = {
                    tentouSalvar = true
                    val temErro = nome.isBlank() ||
                            erroTelefone(telefone, true) != null ||
                            erroCpf(cpf, true) != null ||
                            erroData(dataNascimento, true) != null
                    if (!temErro) {
                        authViewModel.salvarPerfilCompleto(
                            nome = nome.trim(),
                            fotoUrl = fotoUrl,
                            telefone = formatarTelefone(telefone),
                            cpf = formatarCpf(cpf),
                            bio = bio,
                            genero = genero,
                            dataNascimento = dataParaIso(dataNascimento)
                        )
                    }
                },
                enabled = perfilCompletoState !is PerfilCompletoState.Loading && uploadFotoState !is UploadFotoState.Loading,
                loading = perfilCompletoState is PerfilCompletoState.Loading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            SecaoCard(titulo = "Login e segurança") {
                LinhaAcaoConta(
                    rotulo = "E-mail",
                    valorAtual = usuario?.email.orEmpty(),
                    textoBotao = "Trocar",
                    onClick = { mostrarDialogoEmail = true }
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                LinhaAcaoConta(
                    rotulo = "Senha",
                    valorAtual = "••••••••",
                    textoBotao = "Trocar",
                    onClick = { mostrarDialogoSenha = true }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }

    if (mostrarDialogoEmail) {
        DialogoTrocarEmail(
            credenciaisState = credenciaisState,
            onConfirmar = { senhaAtual, novoEmail -> authViewModel.alterarEmail(senhaAtual, novoEmail) },
            onFechar = {
                mostrarDialogoEmail = false
                authViewModel.resetarCredenciaisState()
            }
        )
    }

    if (mostrarDialogoSenha) {
        DialogoTrocarSenha(
            credenciaisState = credenciaisState,
            onConfirmar = { senhaAtual, novaSenha -> authViewModel.alterarSenha(senhaAtual, novaSenha) },
            onFechar = {
                mostrarDialogoSenha = false
                authViewModel.resetarCredenciaisState()
            }
        )
    }
}

@Composable
private fun SecaoCard(titulo: String, conteudo: @Composable ColumnScope.() -> Unit) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.titleSmall,
        color = TextoSecundario,
        modifier = Modifier.padding(bottom = Spacing.sm)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FundoCard, shape = MaterialTheme.shapes.medium)
            .padding(Spacing.md),
        content = conteudo
    )
}

@Composable
private fun LinhaAcaoConta(
    rotulo: String,
    valorAtual: String,
    textoBotao: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(rotulo, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            Text(valorAtual, style = MaterialTheme.typography.bodyLarge, color = TextoPrincipal)
        }
        TextButton(onClick = onClick) {
            Text(textoBotao, color = RoxoNeon)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoTrocarEmail(
    credenciaisState: CredenciaisState,
    onConfirmar: (senhaAtual: String, novoEmail: String) -> Unit,
    onFechar: () -> Unit
) {
    var senhaAtual by remember { mutableStateOf("") }
    var novoEmail by remember { mutableStateOf("") }

    LaunchedEffect(credenciaisState) {
        if (credenciaisState is CredenciaisState.EmailAlterado) onFechar()
    }

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Trocar e-mail") },
        text = {
            Column {
                Text(
                    "Você vai receber um link de confirmação no e-mail novo. A troca só vale depois de confirmar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = novoEmail,
                    onValueChange = { novoEmail = it },
                    label = { Text("Novo e-mail") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = senhaAtual,
                    onValueChange = { senhaAtual = it },
                    label = { Text("Senha atual") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (credenciaisState is CredenciaisState.Erro) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(credenciaisState.mensagem, style = MaterialTheme.typography.bodySmall, color = VermelhoErro)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(senhaAtual, novoEmail) },
                enabled = credenciaisState !is CredenciaisState.Loading
            ) {
                Text(if (credenciaisState is CredenciaisState.Loading) "Enviando..." else "Confirmar", color = RoxoNeon)
            }
        },
        dismissButton = {
            TextButton(onClick = onFechar) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoTrocarSenha(
    credenciaisState: CredenciaisState,
    onConfirmar: (senhaAtual: String, novaSenha: String) -> Unit,
    onFechar: () -> Unit
) {
    var senhaAtual by remember { mutableStateOf("") }
    var novaSenha by remember { mutableStateOf("") }

    LaunchedEffect(credenciaisState) {
        if (credenciaisState is CredenciaisState.SenhaAlterada) onFechar()
    }

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Trocar senha") },
        text = {
            Column {
                OutlinedTextField(
                    value = senhaAtual,
                    onValueChange = { senhaAtual = it },
                    label = { Text("Senha atual") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = novaSenha,
                    onValueChange = { novaSenha = it },
                    label = { Text("Nova senha") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (credenciaisState is CredenciaisState.Erro) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(credenciaisState.mensagem, style = MaterialTheme.typography.bodySmall, color = VermelhoErro)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(senhaAtual, novaSenha) },
                enabled = credenciaisState !is CredenciaisState.Loading
            ) {
                Text(if (credenciaisState is CredenciaisState.Loading) "Salvando..." else "Confirmar", color = RoxoNeon)
            }
        },
        dismissButton = {
            TextButton(onClick = onFechar) { Text("Cancelar") }
        }
    )
}

// ---------- Máscaras, validação e conversões ----------

private fun String.somenteDigitos(): String = filter { it.isDigit() }

/**
 * Aplica uma máscara só na exibição. O estado do campo continua sendo só
 * dígitos; aqui a gente mapeia as posições do cursor entre o texto "cru" e o
 * texto formatado, pra o cursor nunca ficar no lugar errado.
 */
private class MascaraTransformation(private val formatar: (String) -> String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digitos = text.text
        val formatado = formatar(digitos)
        val mapeamento = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset >= digitos.length) return formatado.length
                var contados = 0
                for (i in formatado.indices) {
                    if (formatado[i].isDigit()) {
                        if (contados == offset) return i
                        contados++
                    }
                }
                return formatado.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                val ate = offset.coerceIn(0, formatado.length)
                return formatado.take(ate).count { it.isDigit() }.coerceAtMost(digitos.length)
            }
        }
        return TransformedText(AnnotatedString(formatado), mapeamento)
    }
}

/** (11) 99999-9999 (celular) ou (11) 9999-9999 (fixo). */
private fun formatarTelefone(d: String): String {
    if (d.isEmpty()) return ""
    val ddd = d.take(2)
    val resto = d.drop(2)
    if (resto.isEmpty()) return "($ddd"
    val corte = if (resto.startsWith("9") || d.length > 10) 5 else 4
    val parte1 = resto.take(corte)
    val parte2 = resto.drop(corte)
    return "($ddd) $parte1" + if (parte2.isNotEmpty()) "-$parte2" else ""
}

/** 000.000.000-00 */
private fun formatarCpf(d: String): String = buildString {
    d.forEachIndexed { i, c ->
        when (i) {
            3, 6 -> append('.').append(c)
            9 -> append('-').append(c)
            else -> append(c)
        }
    }
}

/** DD/MM/AAAA */
private fun formatarData(d: String): String = buildString {
    d.forEachIndexed { i, c ->
        when (i) {
            2, 4 -> append('/').append(c)
            else -> append(c)
        }
    }
}

private fun erroTelefone(d: String, tentouSalvar: Boolean): String? {
    if (d.isEmpty()) return null
    val completo = d.length >= 10
    if (!completo) return if (tentouSalvar) "Informe o DDD e o número completo" else null
    val ddd = d.take(2).toIntOrNull() ?: return "Celular inválido"
    if (ddd < 11) return "DDD inválido"
    if (d.length == 11 && d[2] != '9') return "Celular deve começar com 9 depois do DDD"
    return null
}

private fun erroCpf(d: String, tentouSalvar: Boolean): String? {
    if (d.isEmpty()) return null
    if (d.length < 11) return if (tentouSalvar) "O CPF tem 11 números" else null
    return if (cpfValido(d)) null else "CPF inválido"
}

private fun erroData(d: String, tentouSalvar: Boolean): String? {
    if (d.isEmpty()) return null
    if (d.length < 8) return if (tentouSalvar) "Use o formato DD/MM/AAAA" else null
    return if (dataValida(d)) null else "Data inválida"
}

private fun cpfValido(d: String): Boolean {
    if (d.length != 11 || d.all { it == d[0] }) return false
    fun digitoVerificador(base: String, pesoInicial: Int): Int {
        val soma = base.mapIndexed { i, c -> (c - '0') * (pesoInicial - i) }.sum()
        val resto = (soma * 10) % 11
        return if (resto == 10) 0 else resto
    }
    return digitoVerificador(d.substring(0, 9), 10) == (d[9] - '0') &&
            digitoVerificador(d.substring(0, 10), 11) == (d[10] - '0')
}

private fun dataValida(d: String): Boolean {
    if (d.length != 8) return false
    return try {
        val data = LocalDate.of(d.substring(4, 8).toInt(), d.substring(2, 4).toInt(), d.substring(0, 2).toInt())
        data.year >= 1900 && !data.isAfter(LocalDate.now())
    } catch (_: DateTimeException) {
        false
    }
}

/** Cores dos campos: borda roxa no foco, borda sutil em repouso, sem fundo próprio (usa o do card). */
@Composable
private fun coresDoCampo() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextoPrincipal,
    unfocusedTextColor = TextoPrincipal,
    focusedBorderColor = RoxoNeon,
    unfocusedBorderColor = BordaSutil,
    focusedLabelColor = RoxoNeon,
    unfocusedLabelColor = TextoSecundario,
    cursorColor = RoxoNeon,
    focusedPlaceholderColor = TextoSecundario,
    unfocusedPlaceholderColor = TextoSecundario,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
    errorBorderColor = VermelhoErro,
    errorLabelColor = VermelhoErro,
    errorSupportingTextColor = VermelhoErro
)

/** Converte "AAAA-MM-DD" (como vem do banco) pra "DD/MM/AAAA" (como o campo exibe). */
private fun isoParaDataBr(dataIso: String): String {
    val partes = dataIso.take(10).split("-")
    if (partes.size != 3) return ""
    val (ano, mes, dia) = partes
    return "$dia/$mes/$ano"
}

/** Converte os 8 dígitos "DDMMAAAA" pra "AAAA-MM-DD" (formato da coluna date do Postgres). Vazio se incompleto. */
private fun dataParaIso(digitos: String): String {
    if (digitos.length != 8) return ""
    return "${digitos.substring(4, 8)}-${digitos.substring(2, 4)}-${digitos.substring(0, 2)}"
}