package com.example.zeromangas.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.Spacing

/**
 * Botão principal do app (fundo roxo neon).
 * Use para a ação primária da tela: "Adicionar ao carrinho", "Finalizar compra", "Entrar" etc.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(Spacing.radiusSmall),
        colors = ButtonDefaults.buttonColors(
            containerColor = RoxoNeon,
            contentColor = Color.White,
            disabledContainerColor = RoxoNeon.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(horizontal = Spacing.lg)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.height(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * Botão secundário (contorno roxo, fundo transparente).
 * Use para ações alternativas: "Esqueci minha senha", "Cancelar" etc.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(Spacing.radiusSmall)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}
