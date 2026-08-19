package com.example.zeromangas.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoSecundario

/**
 * Indicador de carregamento centralizado, padrão para o app todo.
 * Substitui os CircularProgressIndicator "soltos" espalhados pelas telas.
 */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = RoxoNeon)
    }
}

/**
 * Estado vazio padrão (carrinho vazio, sem favoritos, sem resultados de busca...).
 */
@Composable
fun EmptyState(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    icone: ImageVector = Icons.Outlined.SearchOff
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(Spacing.xl)
        ) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = TextoSecundario,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                color = TextoSecundario
            )
            if (subtitulo != null) {
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
            }
        }
    }
}
