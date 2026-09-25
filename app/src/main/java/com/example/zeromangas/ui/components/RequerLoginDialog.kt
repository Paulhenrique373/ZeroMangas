package com.example.zeromangas.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario

/**
 * Diálogo mostrado quando um visitante (sem conta) tenta usar uma funcionalidade
 * que exige autenticação (finalizar compra, perfil, favoritos, pedidos, etc).
 *
 * Tem as 3 opções pedidas: "Entrar", "Criar conta" e "Continuar navegando"
 * (essa última só fecha o diálogo, sem navegar pra lugar nenhum). Reaproveitado
 * em qualquer ponto do app que precise desse mesmo bloqueio — a decisão de qual
 * tela abrir depois de Entrar/Criar conta é de quem chama (ver NavGraph); este
 * componente só cuida da UI do aviso.
 */
@Composable
fun RequerLoginDialog(
    onEntrar: () -> Unit,
    onCriarConta: () -> Unit,
    onContinuarNavegando: () -> Unit
) {
    Dialog(onDismissRequest = onContinuarNavegando) {
        Surface(color = FundoCard, shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text(
                    "🔐 Crie sua conta para continuar",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextoPrincipal
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Para realizar uma compra ou acessar seus dados, você precisa ter uma conta no ZeroMangas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
                Spacer(Modifier.height(Spacing.md))
                PrimaryButton("Entrar", onEntrar, Modifier.fillMaxWidth())
                Spacer(Modifier.height(Spacing.sm))
                SecondaryButton("Criar conta", onCriarConta, Modifier.fillMaxWidth())
                Spacer(Modifier.height(Spacing.xs))
                TextButton(onClick = onContinuarNavegando, modifier = Modifier.fillMaxWidth()) {
                    Text("Continuar navegando", color = TextoSecundario)
                }
            }
        }
    }
}