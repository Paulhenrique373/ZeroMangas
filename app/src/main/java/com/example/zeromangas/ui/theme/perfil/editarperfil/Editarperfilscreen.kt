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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zeromangas.ui.components.PrimaryButton
import com.example.zeromangas.ui.components.SecondaryButton
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

private val OPCOES_GENERO = listOf("Prefiro não informar", "Feminino", "Masculino", "Não-binário", "Outro")

/**
 * Tela completa de edição de perfil. Reaproveita o [AuthViewModel] já existente
 * (upload de foto, atualização de nome — agora persistido no Supabase também) e
 * adiciona os campos novos (telefone, CPF, bio, gênero, nascimento) + troca de
 * e-mail e senha via Firebase Auth. Não mexe em nenhuma lógica de pedidos/carrinho.
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
    var telefone by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var genero by remember { mutableStateOf("") }
    var dataNascimento by remember { mutableStateOf("") }
    var jaCarregouCampos by remember { mutableStateOf(false) }
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
            telefone = perfilCliente?.telefone.orEmpty()
            cpf = perfilCliente?.cpf.orEmpty()
            bio = perfilCliente?.bio.orEmpty()
            genero = perfilCliente?.genero.orEmpty()
            dataNascimento = isoParaDataBr(perfilCliente?.dataNascimento.orEmpty())
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mostrarSeletorGenero = true }
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
                    onValueChange = { dataNascimento = formatarDataDigitada(it) },
                    label = { Text("Data de nascimento (DD/MM/AAAA)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            SecaoCard(titulo = "Contato e documento") {
                OutlinedTextField(
                    value = telefone,
                    onValueChange = { telefone = formatarTelefoneDigitado(it) },
                    label = { Text("Celular") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Spacing.sm))

                OutlinedTextField(
                    value = cpf,
                    onValueChange = { cpf = formatarCpfDigitado(it) },
                    label = { Text("CPF") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    authViewModel.salvarPerfilCompleto(
                        nome = nome,
                        fotoUrl = fotoUrl,
                        telefone = telefone,
                        cpf = cpf,
                        bio = bio,
                        genero = genero,
                        dataNascimento = dataParaIso(dataNascimento)
                    )
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

// ---------- Formatação leve de campos (só cosmética, não bloqueia digitação) ----------

private fun formatarTelefoneDigitado(entrada: String): String {
    val digitos = entrada.filter { it.isDigit() }.take(11)
    return buildString {
        digitos.forEachIndexed { index, c ->
            when (index) {
                0 -> append("(").append(c)
                1 -> append(c).append(") ")
                7 -> append("-").append(c)
                else -> append(c)
            }
        }
    }
}

private fun formatarCpfDigitado(entrada: String): String {
    val digitos = entrada.filter { it.isDigit() }.take(11)
    return buildString {
        digitos.forEachIndexed { index, c ->
            when (index) {
                3, 6 -> append(".").append(c)
                9 -> append("-").append(c)
                else -> append(c)
            }
        }
    }
}

private fun formatarDataDigitada(entrada: String): String {
    val digitos = entrada.filter { it.isDigit() }.take(8)
    return buildString {
        digitos.forEachIndexed { index, c ->
            when (index) {
                2, 4 -> append("/").append(c)
                else -> append(c)
            }
        }
    }
}

/** Converte "AAAA-MM-DD" (como vem do banco) pra "DD/MM/AAAA" (como o campo exibe). */
private fun isoParaDataBr(dataIso: String): String {
    val partes = dataIso.split("-")
    if (partes.size != 3) return ""
    val (ano, mes, dia) = partes
    return "$dia/$mes/$ano"
}

/** Converte "DD/MM/AAAA" (como digitado) pra "AAAA-MM-DD" (formato aceito pela coluna date do Postgres). */
private fun dataParaIso(dataBr: String): String {
    val partes = dataBr.split("/")
    if (partes.size != 3 || partes.any { it.isBlank() }) return ""
    val (dia, mes, ano) = partes
    if (ano.length != 4) return ""
    return "$ano-${mes.padStart(2, '0')}-${dia.padStart(2, '0')}"
}