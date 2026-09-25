package com.example.zeromangas.ui.theme.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.VerdeSucesso

/**
 * Fase 1 do Painel Administrativo: só confirma que a checagem de permissão
 * (RLS + sou_admin()) está funcionando ponta a ponta. As áreas reais — Dashboard
 * com números da loja, Produtos, Pedidos, etc — entram nas próximas fases, cada
 * uma reaproveitando essa mesma tela como ponto de entrada.
 */
@Composable
fun AdminDashboardScreen(onVoltar: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.screenHorizontal),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoltar) {
                Icon(Icons.Default.ArrowBack, "Voltar", tint = TextoPrincipal)
            }
            Spacer(Modifier.width(Spacing.sm))
            Text("Painel Administrativo", style = MaterialTheme.typography.headlineSmall, color = TextoPrincipal)
        }

        Surface(
            color = FundoCard,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().padding(Spacing.screenHorizontal)
        ) {
            Row(
                modifier = Modifier.padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = VerdeSucesso)
                Spacer(Modifier.width(Spacing.sm))
                Column {
                    Text("Acesso administrativo confirmado", style = MaterialTheme.typography.titleMedium, color = TextoPrincipal)
                    Text(
                        "Dashboard, produtos, pedidos e o resto do painel entram nas próximas etapas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                }
            }
        }
    }
}