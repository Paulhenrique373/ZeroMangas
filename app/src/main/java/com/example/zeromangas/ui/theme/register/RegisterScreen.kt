package com.example.zeromangas.ui.theme.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zeromangas.ui.components.PrimaryButton
import com.example.zeromangas.ui.theme.FundoPrincipal
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.login.CampoSenha
import com.example.zeromangas.ui.theme.login.LogoZeroMangas
import com.example.zeromangas.viewmodel.AuthState
import com.example.zeromangas.viewmodel.AuthViewModel

/**
 * ETAPA 11 (polimento, parte 4): mesmo tratamento do [com.example.zeromangas.ui.theme.login.LoginScreen]
 * — reaproveita [LogoZeroMangas] e [CampoSenha] pra não duplicar código entre as duas telas.
 * Lógica 100% preservada: continua chamando authViewModel.cadastrar(nome, email, senha).
 *
 * O campo "Confirmar senha" e a recuperação de senha ("Esqueci minha senha") do plano original
 * NÃO foram adicionados aqui — são funcionalidades novas (exigiriam validação/lógica nova no
 * ViewModel), fora do escopo de polimento da Etapa 11. Ficam como sugestão pra uma etapa futura.
 */
@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel = viewModel(),
    onCadastroSucesso: () -> Unit,
    onVoltarParaLogin: () -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Sucesso) {
            onCadastroSucesso()
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
                text = "Junte-se ao ZeroMangás",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.md))

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
                text = "Cadastrar",
                onClick = { authViewModel.cadastrar(nome, email, senha) },
                enabled = authState !is AuthState.Loading,
                loading = authState is AuthState.Loading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            TextButton(onClick = onVoltarParaLogin) {
                Text("Já tem conta? Entrar", color = RoxoNeon)
            }
        }
    }
}