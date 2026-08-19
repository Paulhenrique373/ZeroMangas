package com.example.zeromangas.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Escala de espaçamentos padronizada do ZeroMangas.
 * Use sempre estes valores em vez de números "soltos" (magic numbers)
 * para manter o layout consistente entre todas as telas.
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp

    // Raios de borda
    val radiusSmall = 8.dp
    val radiusMedium = 16.dp
    val radiusLarge = 20.dp
    val radiusPill = 100.dp // usado em chips/botões arredondados totalmente

    // Dimensões recorrentes de componentes
    val mangaCoverWidth = 130.dp
    val mangaCoverHeight = 180.dp
    val categoryIconSize = 56.dp
    val bottomNavHeight = 72.dp
}
