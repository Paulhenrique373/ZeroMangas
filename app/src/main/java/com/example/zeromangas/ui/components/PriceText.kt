package com.example.zeromangas.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import com.example.zeromangas.ui.theme.AmareloDestaque
import com.example.zeromangas.ui.theme.RoxoNeonClaro
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoSecundario
import java.text.NumberFormat
import java.util.Locale

/**
 * Formata um valor Double para o padrão de moeda brasileiro (R$ 39,90).
 */
fun formatarPrecoBr(valor: Double): String {
    val formatador = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatador.format(valor)
}

/**
 * Exibe o preço de um produto. Se [precoAntigo] for informado e maior que [preco],
 * mostra o preço antigo riscado acima do preço atual em destaque, além de um
 * selinho de desconto opcional.
 */
@Composable
fun PriceText(
    preco: Double,
    modifier: Modifier = Modifier,
    precoAntigo: Double? = null
) {
    val temDesconto = precoAntigo != null && precoAntigo > preco

    Column(modifier = modifier) {
        if (temDesconto) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatarPrecoBr(precoAntigo!!),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario,
                    textDecoration = TextDecoration.LineThrough
                )
                val desconto = (((precoAntigo - preco) / precoAntigo) * 100).toInt()
                Text(
                    text = "  -$desconto%",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmareloDestaque,
                    modifier = Modifier.padding(start = Spacing.xs)
                )
            }
        }
        Text(
            text = formatarPrecoBr(preco),
            style = MaterialTheme.typography.labelLarge,
            color = if (temDesconto) AmareloDestaque else RoxoNeonClaro
        )
    }
}
