package com.example.zeromangas.ui.theme.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zeromangas.ui.components.PrimaryButton
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.FundoPrincipal
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.login.AuthError
import com.example.zeromangas.ui.theme.login.CampoSenha
import com.example.zeromangas.ui.theme.login.LogoZeroMangas
import com.example.zeromangas.viewmodel.AuthState
import com.example.zeromangas.viewmodel.AuthViewModel

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
    val foco = LocalFocusManager.current
    val cadastrar = {
        foco.clearFocus()
        authViewModel.cadastrar(nome.trim(), email.trim(), senha)
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Sucesso) {
            authViewModel.resetarEstado()
            onCadastroSucesso()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(FundoPrincipal).imePadding(), contentAlignment = Alignment.Center) {
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
                Text("Crie sua conta", style = MaterialTheme.typography.headlineMedium, color = TextoPrincipal)
                Text("Monte sua biblioteca e acompanhe seus pedidos.", style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
            }
            Spacer(Modifier.height(Spacing.lg))

            Surface(color = FundoCard, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it; if (authState is AuthState.Erro) authViewModel.resetarEstado() },
                        label = { Text("Nome") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Spacing.md))
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
                        onDone = cadastrar
                    )
                    if (authState is AuthState.Erro) {
                        Spacer(Modifier.height(Spacing.sm))
                        AuthError((authState as AuthState.Erro).mensagem)
                    }
                    Spacer(Modifier.height(Spacing.lg))
                    PrimaryButton("Cadastrar", cadastrar, Modifier.fillMaxWidth(), enabled = authState !is AuthState.Loading, loading = authState is AuthState.Loading)
                }
            }
            Spacer(Modifier.height(Spacing.md))
            TextButton(onClick = onVoltarParaLogin) {
                Text("Já tem conta? ")
                Text("Entrar", color = RoxoNeon)
            }
        }
    }
}
