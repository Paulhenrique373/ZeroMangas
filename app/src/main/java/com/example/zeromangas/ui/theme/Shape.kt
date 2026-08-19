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
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(Spacing.radiusMedium),
    large = RoundedCornerShape(Spacing.radiusLarge),
    extraLarge = RoundedCornerShape(28.dp)
)
