package com.example.zeromangas.ui.theme

import androidx.compose.ui.graphics.Color

// Identidade ZeroMangás: roxo sofisticado, usado como destaque — nunca como fundo dominante.
val RoxoNeon = Color(0xFFA855F7)
val RoxoNeonClaro = Color(0xFFC084FC)
val RoxoNeonEscuro = Color(0xFF7E22CE)

// Tons de fundo (tema escuro premium)
val FundoPrincipal = Color(0xFF0F0F12)
val FundoCard = Color(0xFF1C1C22)
val FundoCardClaro = Color(0xFF26262E)

// Superfícies e feedbacks semânticos. Mantidos separados dos nomes de domínio para
// que o tema Material 3 consiga distribuir as cores de modo consistente.
val FundoElevado = FundoCardClaro
val SobreposicaoPrimaria = Color(0xFF2D1B42)
val SobreposicaoErro = Color(0xFF3B171B)

// Borda discreta usada em cards e superfícies elevadas (ETAPA 1 - design system).
// Bem sutil de propósito: não deve competir com o roxo, só separar o card do fundo.
val BordaSutil = Color(0xFF3A3A45)
val BordaFocada = RoxoNeonClaro

// Texto
val TextoPrincipal = Color(0xFFF5F5F7)
val TextoSecundario = Color(0xFFA1A1AA)

// Cores de apoio
val VerdeSucesso = Color(0xFF22C55E)
val VermelhoErro = Color(0xFFEF4444)
val AmareloDestaque = Color(0xFFFACC15)
