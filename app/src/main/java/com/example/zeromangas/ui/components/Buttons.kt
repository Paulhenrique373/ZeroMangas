package com.example.zeromangas.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.Spacing

/**
 * Botão principal do app (fundo roxo neon).
 * Use para a ação primária da tela: "Adicionar ao carrinho", "Finalizar compra", "Entrar" etc.
 *
 * ETAPA 11 (polimento): ganhou uma leve animação de "encolher" ao ser pressionado
 * (via [MutableInteractionSource] + [animateFloatAsState]), pra dar feedback tátil
 * sem precisar de nenhuma lib nova. Não muda a lógica de clique/loading existente.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressionado by interactionSource.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (pressionado) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "escalaPrimaryButton"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .scale(escala),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(Spacing.radiusSmall),
        colors = ButtonDefaults.buttonColors(
            containerColor = RoxoNeon,
            contentColor = Color.White,
            disabledContainerColor = RoxoNeon.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        interactionSource = interactionSource
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
 *
 * Mesma animação de "encolher ao pressionar" do [PrimaryButton], pra manter o
 * app inteiro com o mesmo tipo de feedback tátil.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressionado by interactionSource.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (pressionado) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "escalaSecondaryButton"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .scale(escala),
        enabled = enabled,
        shape = RoundedCornerShape(Spacing.radiusSmall),
        interactionSource = interactionSource
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}