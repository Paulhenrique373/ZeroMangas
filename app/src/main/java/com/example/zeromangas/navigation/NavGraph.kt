package com.example.zeromangas.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.zeromangas.repository.AuthRepository
import com.example.zeromangas.repository.MangaRepository
import com.example.zeromangas.ui.detalhes.DetalhesScreen
import com.example.zeromangas.ui.home.HomeScreen
import com.example.zeromangas.ui.login.LoginScreen
import com.example.zeromangas.ui.register.RegisterScreen
import com.example.zeromangas.ui.theme.cart.CartScreen
import com.example.zeromangas.viewmodel.CartViewModel

sealed class Tela(val rota: String) {
    object Login : Tela("login")
    object Cadastro : Tela("cadastro")
    object Home : Tela("home")
    object Carrinho : Tela("carrinho")
    object Detalhes : Tela("detalhes/{mangaId}") {
        fun criarRota(mangaId: String) = "detalhes/$mangaId"
    }
}

@Composable
fun NavGraph() {
    val navController: NavHostController = rememberNavController()
    val mangaRepository = MangaRepository()
    val authRepository = AuthRepository()
    val cartViewModel: CartViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Tela.Login.rota
    ) {
        composable(Tela.Login.rota) {
            LoginScreen(
                onLoginSucesso = {
                    navController.navigate(Tela.Home.rota) {
                        popUpTo(Tela.Login.rota) { inclusive = true }
                    }
                },
                onIrParaCadastro = {
                    navController.navigate(Tela.Cadastro.rota)
                }
            )
        }

        composable(Tela.Cadastro.rota) {
            RegisterScreen(
                onCadastroSucesso = {
                    navController.navigate(Tela.Home.rota) {
                        popUpTo(Tela.Login.rota) { inclusive = true }
                    }
                },
                onVoltarParaLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Tela.Home.rota) {
            HomeScreen(
                cartViewModel = cartViewModel,
                onMangaClick = { manga ->
                    navController.navigate(Tela.Detalhes.criarRota(manga.id))
                },
                onCarrinhoClick = {
                    navController.navigate(Tela.Carrinho.rota)
                }
            )
        }

        composable(
            route = Tela.Detalhes.rota,
            arguments = listOf(navArgument("mangaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val mangaId = backStackEntry.arguments?.getString("mangaId") ?: ""
            val manga = mangaRepository.listarMangas().find { it.id == mangaId }

            DetalhesScreen(
                manga = manga,
                onVoltar = { navController.popBackStack() },
                onAdicionarAoCarrinho = { mangaSelecionado ->
                    cartViewModel.adicionarItem(mangaSelecionado)
                    navController.popBackStack()
                }
            )
        }

        composable(Tela.Carrinho.rota) {
            CartScreen(
                cartViewModel = cartViewModel,
                usuarioId = authRepository.currentUser?.uid.orEmpty(),
                onVoltar = { navController.popBackStack() },
                onFinalizarCompra = {
                    navController.navigate(Tela.Home.rota) {
                        popUpTo(Tela.Home.rota) { inclusive = true }
                    }
                }
            )
        }
    }
}