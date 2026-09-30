package com.example.zeromangas.ui.theme.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.VerdeSucesso
import com.example.zeromangas.viewmodel.AuthViewModel
import com.example.zeromangas.viewmodel.RecuperacaoSenhaState

/**
 * "Esqueci minha senha" em duas etapas, dentro de um diálogo (não precisa de
 * rota nova): 1) e-mail -> manda o código; 2) código + nova senha.
 */
@Composable
fun RecuperarSenhaDialog(
    authViewModel: AuthViewModel,
    emailInicial: String,
    onFechar: () -> Unit
) {
    val estado by authViewModel.recuperacaoSenhaState.collectAsState()

    var email by remember { mutableStateOf(emailInicial.trim()) }
    var codigo by remember { mutableStateOf("") }
    var novaSenha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }

    // Sempre começa do passo 1 e não deixa estado velho pra próxima abertura.
    DisposableEffect(Unit) {
        authViewModel.resetarRecuperacaoSenha()
        onDispose { authViewModel.resetarRecuperacaoSenha() }
    }

    val naEtapaDoCodigo = estado is RecuperacaoSenhaState.CodigoEnviado ||
            estado is RecuperacaoSenhaState.Redefinindo ||
            (estado is RecuperacaoSenhaState.Erro && (estado as RecuperacaoSenhaState.Erro).aoRedefinir)
    val sucesso = estado is RecuperacaoSenhaState.Sucesso
    val ocupado = estado is RecuperacaoSenhaState.Enviando || estado is RecuperacaoSenhaState.Redefinindo

    AlertDialog(
        onDismissRequest = { if (!ocupado) onFechar() },
        title = {
            Text(
                when {
                    sucesso -> "Senha alterada"
                    naEtapaDoCodigo -> "Digite o código"
                    else -> "Recuperar senha"
                }
            )
        },
        text = {
            Column {
                when {
                    sucesso -> Text(
                        "Pronto! Agora é só entrar com a sua senha nova.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VerdeSucesso
                    )

                    naEtapaDoCodigo -> {
                        Text(
                            "Se $email estiver cadastrado, enviamos um código por e-mail. Ele vale por pouco tempo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSecundario
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        OutlinedTextField(
                            value = codigo,
                            onValueChange = { codigo = it.filter(Char::isDigit).take(10) },
                            label = { Text("Código do e-mail") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        CampoSenha(
                            senha = novaSenha,
                            onSenhaChange = { novaSenha = it },
                            visivel = senhaVisivel,
                            onToggleVisivel = { senhaVisivel = !senhaVisivel },
                            label = "Nova senha",
                            imeAction = ImeAction.Done,
                            onDone = { authViewModel.redefinirSenhaComCodigo(email, codigo, novaSenha) }
                        )
                    }

                    else -> {
                        Text(
                            "Informe o e-mail da sua conta. Vamos enviar um código pra você criar uma senha nova.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSecundario
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("E-mail") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                val erro = estado as? RecuperacaoSenhaState.Erro
                if (erro != null) {
                    Spacer(Modifier.height(Spacing.sm))
                    AuthError(erro.mensagem)
                }
            }
        },
        confirmButton = {
            when {
                sucesso -> TextButton(onClick = onFechar) { Text("Entrar", color = RoxoNeon) }

                naEtapaDoCodigo -> TextButton(
                    onClick = { authViewModel.redefinirSenhaComCodigo(email, codigo, novaSenha) },
                    enabled = !ocupado
                ) {
                    Text(if (estado is RecuperacaoSenhaState.Redefinindo) "Salvando..." else "Redefinir senha", color = RoxoNeon)
                }

                else -> TextButton(
                    onClick = { authViewModel.enviarCodigoRecuperacao(email) },
                    enabled = !ocupado
                ) {
                    Text(if (estado is RecuperacaoSenhaState.Enviando) "Enviando..." else "Enviar código", color = RoxoNeon)
                }
            }
        },
        dismissButton = {
            if (!sucesso) {
                TextButton(onClick = onFechar, enabled = !ocupado) { Text("Cancelar") }
            }
        }
    )
}