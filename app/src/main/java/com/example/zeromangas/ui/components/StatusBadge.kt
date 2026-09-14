package com.example.zeromangas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.zeromangas.ui.theme.AmareloDestaque
import com.example.zeromangas.ui.theme.RoxoNeonClaro
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.VerdeSucesso
import com.example.zeromangas.ui.theme.VermelhoErro

/**
 * Retorna a cor semântica de um status de pedido exibido na interface.
 * Extraída de PedidosScreen.kt na ETAPA 1 (design system) para virar componente
 * global reutilizável, exatamente com o mesmo mapeamento de cores de antes.
 */
fun corDoStatusPedido(status: String): Color {
    return when (status) {
        "Pedido confirmado", "Preparando" -> AmareloDestaque
        "Enviado" -> RoxoNeonClaro
        "Cancelado" -> VermelhoErro
        else -> VerdeSucesso // Entregue
    }
}

/**
 * Badge "● Status" com uma bolinha colorida, no padrão pedido no planejamento (🟡 Preparando).
 * Usado hoje em PedidosScreen; fica disponível em ui/components para outras telas
 * (ex: acompanhamento de pedido / detalhes) reaproveitarem sem duplicar código.
 */
@Composable
fun StatusBadge(status: String) {
    val cor = corDoStatusPedido(status)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Spacing.radiusPill))
            .background(cor.copy(alpha = 0.15f))
            .padding(horizontal = Spacing.sm, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(cor)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = status, style = MaterialTheme.typography.labelSmall, color = cor, fontWeight = FontWeight.Medium)
    }
}
