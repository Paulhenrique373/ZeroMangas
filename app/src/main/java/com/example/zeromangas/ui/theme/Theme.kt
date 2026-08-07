package com.example.zeromangas.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RoxoNeon,
    secondary = RoxoNeonClaro,
    tertiary = RoxoNeonEscuro,
    background = FundoPrincipal,
    surface = FundoCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextoPrincipal,
    onSurface = TextoPrincipal,
    error = VermelhoErro
)

private val LightColorScheme = lightColorScheme(
    primary = RoxoNeon,
    secondary = RoxoNeonClaro,
    tertiary = RoxoNeonEscuro
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
        content = content
    )
}