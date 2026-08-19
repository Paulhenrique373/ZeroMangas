package com.example.zeromangas.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.TextoSecundario

/**
 * Abas da navegação inferior do app. A rota de cada aba é a mesma
 * usada em [com.example.zeromangas.navigation.Tela], mantidas aqui como
 * String simples para não criar dependência circular entre os pacotes.
 */
enum class AbaPrincipal(
    val rota: String,
    val rotulo: String,
    val iconeSelecionado: ImageVector,
    val iconeNaoSelecionado: ImageVector
) {
    HOME("home", "Início", Icons.Filled.Home, Icons.Outlined.Home),
    BUSCA("busca", "Buscar", Icons.Filled.Search, Icons.Outlined.Search),
    CARRINHO("carrinho", "Carrinho", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
    FAVORITOS("favoritos", "Favoritos", Icons.Filled.Favorite, Icons.Outlined.Favorite),
    PERFIL("perfil", "Perfil", Icons.Filled.Person, Icons.Outlined.Person)
}

/**
 * Bottom navigation padrão do ZeroMangás. Usado pelo NavGraph, que decide
 * (com base na rota atual) em quais telas ela deve aparecer.
 *
 * [quantidadeNoCarrinho] alimenta o badge do item Carrinho — reaproveita o
 * mesmo valor já calculado a partir do CartViewModel na Home.
 */
@Composable
fun BottomNavBar(
    rotaAtual: String?,
    quantidadeNoCarrinho: Int,
    onAbaSelecionada: (AbaPrincipal) -> Unit
) {
    NavigationBar(
        containerColor = FundoCard,
        contentColor = TextoSecundario
    ) {
        AbaPrincipal.entries.forEach { aba ->
            val selecionado = rotaAtual == aba.rota

            NavigationBarItem(
                selected = selecionado,
                onClick = { onAbaSelecionada(aba) },
                icon = {
                    if (aba == AbaPrincipal.CARRINHO && quantidadeNoCarrinho > 0) {
                        BadgedBox(
                            badge = { Badge { Text("$quantidadeNoCarrinho") } }
                        ) {
                            Icon(
                                imageVector = if (selecionado) aba.iconeSelecionado else aba.iconeNaoSelecionado,
                                contentDescription = aba.rotulo
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (selecionado) aba.iconeSelecionado else aba.iconeNaoSelecionado,
                            contentDescription = aba.rotulo
                        )
                    }
                },
                label = { Text(aba.rotulo, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = RoxoNeon,
                    selectedTextColor = RoxoNeon,
                    unselectedIconColor = TextoSecundario,
                    unselectedTextColor = TextoSecundario,
                    indicatorColor = FundoCard
                )
            )
        }
    }
}