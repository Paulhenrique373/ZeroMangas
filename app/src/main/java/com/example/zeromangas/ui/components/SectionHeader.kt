package com.example.zeromangas.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.zeromangas.ui.theme.RoxoNeonClaro
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal

/**
 * Cabeçalho de uma seção horizontal da Home (ex: "🔥 Mais vendidos   Ver todos >").
 * [onVerTodosClick] é opcional: se nulo, o link "Ver todos" não é exibido.
 */
@Composable
fun SectionHeader(
    titulo: String,
    modifier: Modifier = Modifier,
    onVerTodosClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            color = TextoPrincipal
        )
        if (onVerTodosClick != null) {
            Text(
                text = "Ver todos >",
                style = MaterialTheme.typography.bodyMedium,
                color = RoxoNeonClaro,
                modifier = Modifier.clickable { onVerTodosClick() }
            )
        }
    }
}
