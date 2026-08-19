package com.example.zeromangas.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.zeromangas.ui.theme.AmareloDestaque
import com.example.zeromangas.ui.theme.TextoSecundario
import kotlin.math.floor

/**
 * Exibe uma avaliação em estrelas (0.0 a 5.0), com o valor numérico ao lado.
 * Ex: ⭐⭐⭐⭐⭒ 4.8
 */
@Composable
fun RatingStars(
    nota: Double,
    modifier: Modifier = Modifier,
    mostrarValor: Boolean = true,
    tamanhoEstrela: Dp = 16.dp
) {
    val notaLimitada = nota.coerceIn(0.0, 5.0)
    val estrelasCheias = floor(notaLimitada).toInt()
    val temMeiaEstrela = (notaLimitada - estrelasCheias) >= 0.5

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(5) { indice ->
            val icone = when {
                indice < estrelasCheias -> Icons.Filled.Star
                indice == estrelasCheias && temMeiaEstrela -> Icons.Filled.StarHalf
                else -> Icons.Outlined.StarOutline
            }
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = AmareloDestaque,
                modifier = Modifier.size(tamanhoEstrela)
            )
        }
        if (mostrarValor) {
            Text(
                text = String.format("%.1f", notaLimitada),
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
