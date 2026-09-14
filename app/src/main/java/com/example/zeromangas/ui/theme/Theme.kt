package com.example.zeromangas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RoxoNeon,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = SobreposicaoPrimaria,
    onPrimaryContainer = RoxoNeonClaro,
    secondary = RoxoNeonClaro,
    onSecondary = FundoPrincipal,
    secondaryContainer = FundoElevado,
    onSecondaryContainer = TextoPrincipal,
    tertiary = RoxoNeonEscuro,
    onTertiary = Color(0xFFFFFFFF),
    background = FundoPrincipal,
    onBackground = TextoPrincipal,
    surface = FundoCard,
    onSurface = TextoPrincipal,
    surfaceVariant = FundoElevado,
    onSurfaceVariant = TextoSecundario,
    surfaceContainerHighest = FundoElevado,
    outline = BordaSutil,
    outlineVariant = BordaSutil.copy(alpha = 0.65f),
    error = VermelhoErro,
    onError = Color(0xFFFFFFFF),
    errorContainer = SobreposicaoErro,
    onErrorContainer = Color(0xFFFFB4AB),
    scrim = Color(0xFF000000)
)

private val LightColorScheme = lightColorScheme(
    primary = RoxoNeonEscuro,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0E3FF),
    onPrimaryContainer = RoxoNeonEscuro,
    secondary = RoxoNeon,
    onSecondary = Color.White,
    tertiary = RoxoNeonClaro,
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE8E0EA),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    error = VermelhoErro
)

@Composable
fun ZeroMangasTheme(
    darkTheme: Boolean = true, // Forçado escuro por padrão - é a identidade visual do app
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ZeroMangasShapes,
        content = content
    )
}
