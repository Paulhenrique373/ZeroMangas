package com.example.zeromangas.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.zeromangas.ui.theme.FundoCardClaro
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario

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
        shape = RoundedCornerShape(Spacing.radiusPill),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = FundoCardClaro,
            labelColor = TextoSecundario,
            selectedContainerColor = RoxoNeon,
            selectedLabelColor = TextoPrincipal
        )
    )
}
