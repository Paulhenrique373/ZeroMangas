package com.example.zeromangas.ui.theme.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zeromangas.ui.components.PrimaryButton
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.FundoPrincipal
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.viewmodel.AuthState
import com.example.zeromangas.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSucesso: () -> Unit,
    onIrParaCadastro: () -> Unit,
    onContinuarSemConta: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.collectAsState()
    val foco = LocalFocusManager.current
    val entrar = {
        foco.clearFocus()
        authViewModel.login(email.trim(), senha)
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Sucesso) {
            authViewModel.resetarEstado()
            onLoginSucesso()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(FundoPrincipal).imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(
                horizontal = Spacing.screenHorizontal,
                vertical = Spacing.xl
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoZeroMangas()
            Spacer(Modifier.height(Spacing.xl))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Bem-vindo de volta", style = MaterialTheme.typography.headlineMedium, color = TextoPrincipal)
                Text("Entre para continuar sua coleção.", style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
            }
            Spacer(Modifier.height(Spacing.lg))

            Surface(color = FundoCard, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; if (authState is AuthState.Erro) authViewModel.resetarEstado() },
                        label = { Text("E-mail") },
                        placeholder = { Text("voce@email.com") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Spacing.md))
                    CampoSenha(
                        senha = senha,
                        onSenhaChange = { senha = it; if (authState is AuthState.Erro) authViewModel.resetarEstado() },
                        visivel = senhaVisivel,
                        onToggleVisivel = { senhaVisivel = !senhaVisivel },
                        imeAction = ImeAction.Done,
                        onDone = entrar
                    )
                    if (authState is AuthState.Erro) {
                        Spacer(Modifier.height(Spacing.sm))
                        AuthError((authState as AuthState.Erro).mensagem)
                    }
                    Spacer(Modifier.height(Spacing.lg))
                    PrimaryButton("Entrar", entrar, Modifier.fillMaxWidth(), enabled = authState !is AuthState.Loading, loading = authState is AuthState.Loading)
                }
            }
            Spacer(Modifier.height(Spacing.md))
            TextButton(onClick = onIrParaCadastro) {
                Text("Ainda não tem conta? ")
                Text("Cadastrar", color = RoxoNeon)
            }

            Spacer(Modifier.height(Spacing.sm))
            SeparadorOu()
            Spacer(Modifier.height(Spacing.sm))
            TextButton(onClick = onContinuarSemConta) {
                Text("Continuar sem conta", color = TextoSecundario)
            }
        }
    }
}

/** Divisor "── ou ──" usado para não competir visualmente com o botão principal de login. */
@Composable
private fun SeparadorOu() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "ou",
            style = MaterialTheme.typography.labelMedium,
            color = TextoSecundario,
            modifier = Modifier.padding(horizontal = Spacing.sm)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
internal fun LogoZeroMangas() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(color = RoxoNeon.copy(alpha = .16f), shape = RoundedCornerShape(Spacing.radiusLarge), modifier = Modifier.size(72.dp)) {
            Icon(Icons.Default.AutoStories, null, tint = RoxoNeon, modifier = Modifier.padding(18.dp))
        }
        Spacer(Modifier.height(Spacing.sm))
        Text("ZeroMangás", style = MaterialTheme.typography.headlineSmall, color = TextoPrincipal)
    }
}

@Composable
internal fun CampoSenha(
    senha: String,
    onSenhaChange: (String) -> Unit,
    visivel: Boolean,
    onToggleVisivel: () -> Unit,
    label: String = "Senha",
    imeAction: ImeAction = ImeAction.Done,
    onDone: () -> Unit = {}
) {
    OutlinedTextField(
        value = senha,
        onValueChange = onSenhaChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Default.Lock, null) },
        trailingIcon = {
            IconButton(onClick = onToggleVisivel) {
                Icon(if (visivel) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (visivel) "Ocultar senha" else "Mostrar senha", tint = TextoSecundario)
            }
        },
        visualTransformation = if (visivel) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun AuthError(mensagem: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
        Text(mensagem, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Start, modifier = Modifier.padding(Spacing.sm))
    }
}