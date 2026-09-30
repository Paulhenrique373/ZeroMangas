package com.example.zeromangas.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.zeromangas.data.config.LojaConfig
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario

/**
 * Cartão com o ponto local do e-commerce (endereço da loja) e um botão que abre
 * o endereço no mapa. Reutilizável em qualquer tela (checkout, perfil, etc.).
 */
@Composable
fun LojaCard(
    modifier: Modifier = Modifier,
    legenda: String = "Seus pedidos saem daqui"
) {
    val context = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.Start) {
                Icon(
                    Icons.Default.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Spacing.iconLarge)
                )
                Spacer(Modifier.width(Spacing.sm))
                Column {
                    Text(LojaConfig.NOME, style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
                    Text(legenda, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    Spacer(Modifier.height(Spacing.xs))
                    Text(LojaConfig.ENDERECO_LINHA_1, style = MaterialTheme.typography.bodyMedium, color = TextoPrincipal)
                    Text(LojaConfig.ENDERECO_LINHA_2, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            OutlinedButton(
                onClick = { LojaConfig.abrirNoMapa(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Map, null, modifier = Modifier.size(Spacing.iconMedium))
                Spacer(Modifier.width(Spacing.xs))
                Text("Ver no mapa")
            }
        }
    }
}