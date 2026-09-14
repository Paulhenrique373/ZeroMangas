package com.example.zeromangas.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.zeromangas.ui.theme.FundoCardClaro
import com.example.zeromangas.ui.theme.Spacing

/**
 * Chip usado para categorias (Shounen, Seinen, Ação...) na Home e na Busca.
 * Quando [selecionado] é true, fica destacado em roxo neon.
 */
@Composable
fun CategoryChip(
    texto: String,
    selecionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selecionado,
        onClick = onClick,
        label = {
            Text(
                text = texto,
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = modifier.padding(end = Spacing.xs),
        shape = MaterialTheme.shapes.extraLarge,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = FundoCardClaro,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            selectedLabelColor = MaterialTheme.colorScheme.primary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selecionado,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        )
    )
}
