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

    // Espaços de composição: margem horizontal de tela, grupos e itens internos.
    val screenHorizontal = 20.dp
    val sectionGap = 28.dp
    val itemGap = 12.dp

    // Raios de borda
    val radiusSmall = 12.dp
    val radiusMedium = 16.dp
    val radiusLarge = 20.dp
    val radiusPill = 100.dp // usado em chips/botões arredondados totalmente

    // Dimensões recorrentes de componentes
    val buttonHeight = 52.dp
    val compactButtonHeight = 40.dp
    val textFieldMinHeight = 56.dp
    val iconSmall = 16.dp
    val iconMedium = 20.dp
    val iconLarge = 24.dp
    val touchTarget = 48.dp
    val borderWidth = 1.dp
    val subtleElevation = 1.dp
    // A vitrine horizontal prioriza dois produtos legíveis por vez e uma prévia do
    // próximo item; três cards estreitos deixam títulos e preços comprimidos.
    val mangaCoverWidth = 190.dp
    val mangaCoverHeight = 180.dp
    val categoryIconSize = 56.dp
    val bottomNavHeight = 72.dp
}
