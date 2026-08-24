package com.example.zeromangas.ui.theme.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zeromangas.ui.components.PrimaryButton
import com.example.zeromangas.ui.theme.FundoPrincipal
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.viewmodel.AuthState
import com.example.zeromangas.viewmodel.AuthViewModel

/**
 * ETAPA 11 (polimento, parte 4): Login e Cadastro nunca tinham recebido o design
 * system (continuavam com o Button/OutlinedTextField "cru" da Etapa 0). Aqui elas
 * ganham: PrimaryButton (mesmo feedback tátil do resto do app), um ícone de marca em
 * vez de só texto solto, e o toggle de mostrar/ocultar senha que já estava previsto
 * desde o planejamento original mas nunca tinha sido implementado. Nenhuma linha do
 * [AuthViewModel] foi alterada — login(email, senha) e o AuthState continuam iguais.
 */
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSucesso: () -> Unit,
    onIrParaCadastro: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Sucesso) {
            onLoginSucesso()
            authViewModel.resetarEstado()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoPrincipal)
            .padding(Spacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            LogoZeroMangas()

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "Entre na sua conta",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            CampoSenha(
                senha = senha,
                onSenhaChange = { senha = it },
                visivel = senhaVisivel,
                onToggleVisivel = { senhaVisivel = !senhaVisivel }
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            if (authState is AuthState.Erro) {
                Text(
                    text = (authState as AuthState.Erro).mensagem,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            PrimaryButton(
                text = "Entrar",
                onClick = { authViewModel.login(email, senha) },
                enabled = authState !is AuthState.Loading,
                loading = authState is AuthState.Loading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            TextButton(onClick = onIrParaCadastro) {
                Text("Não tem conta? Cadastre-se", color = RoxoNeon)
            }
        }
    }
}

/**
 * Ícone + nome do app, reaproveitado no Login e no Cadastro pra dar uma identidade
 * visual mínima nas duas telas (antes era só um Text solto).
 */
@Composable
internal fun LogoZeroMangas() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(Spacing.radiusLarge))
            .background(RoxoNeon.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoStories,
            contentDescription = null,
            tint = RoxoNeon,
            modifier = Modifier.size(36.dp)
        )
    }
    Spacer(modifier = Modifier.height(Spacing.sm))
    Text(
        text = "ZeroMangás",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = TextoPrincipal
    )
}

/**
 * Campo de senha com botão de olho pra mostrar/ocultar o texto digitado.
 * Usado no Login e no Cadastro (e, no Cadastro, também no campo "Confirmar senha").
 */
@Composable
internal fun CampoSenha(
    senha: String,
    onSenhaChange: (String) -> Unit,
    visivel: Boolean,
    onToggleVisivel: () -> Unit,
    label: String = "Senha"
) {
    OutlinedTextField(
        value = senha,
        onValueChange = onSenhaChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            val icone: ImageVector = if (visivel) Icons.Default.VisibilityOff else Icons.Default.Visibility
            val descricao = if (visivel) "Ocultar senha" else "Mostrar senha"
            IconButton(onClick = onToggleVisivel) {
                Icon(icone, contentDescription = descricao, tint = TextoSecundario)
            }
        },
        visualTransformation = if (visivel) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}