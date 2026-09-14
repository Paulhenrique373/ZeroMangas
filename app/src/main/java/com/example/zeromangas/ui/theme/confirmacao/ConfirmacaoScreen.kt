package com.example.zeromangas.ui.theme.confirmacao

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.zeromangas.ui.components.PrimaryButton
import com.example.zeromangas.ui.components.SecondaryButton
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario

@Composable
fun ConfirmacaoScreen(
    pedidoId: String,
    onVoltarParaHome: () -> Unit,
    onVerPedidos: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = .14f),
            shape = CircleShape,
            modifier = Modifier.size(88.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(18.dp)
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Text("Pedido confirmado", style = MaterialTheme.typography.headlineMedium, color = TextoPrincipal, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "Sua compra foi confirmada e já está sendo preparada.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.lg))
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
            Column(modifier = Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NÚMERO DO PEDIDO", style = MaterialTheme.typography.labelSmall, color = TextoSecundario)
                Spacer(Modifier.height(Spacing.xs))
                Text("#${pedidoId.takeLast(6).uppercase()}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(Spacing.xxl))
        PrimaryButton("Ver meus pedidos", onVerPedidos, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.sm))
        SecondaryButton("Continuar comprando", onVoltarParaHome, Modifier.fillMaxWidth())
    }
}
