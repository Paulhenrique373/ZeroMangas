package com.example.zeromangas.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formas arredondadas padrão do app, registradas no MaterialTheme.
 * extraSmall -> chips pequenos
 * small      -> botões
 * medium     -> cards de produto
 * large      -> banners, bottom sheets
 * extraLarge -> imagens de destaque (capa grande na tela de detalhes)
 */
val ZeroMangasShapes = Shapes(
    extraSmall = RoundedCornerShape(Spacing.radiusSmall),
    small = RoundedCornerShape(Spacing.radiusSmall),
    medium = RoundedCornerShape(Spacing.radiusMedium),
    large = RoundedCornerShape(Spacing.radiusLarge),
    extraLarge = RoundedCornerShape(28.dp)
)
