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
import com.example.zeromangas.ui.theme.confirmacao.ConfirmacaoScreen
import com.example.zeromangas.ui.theme.pedidos.PedidosScreen
import com.example.zeromangas.ui.perfil.ProfileScreen
import com.example.zeromangas.viewmodel.AuthViewModel
import com.example.zeromangas.viewmodel.CartViewModel

sealed class Tela(val rota: String) {
    object Login : Tela("login")
    object Cadastro : Tela("cadastro")
    object Home : Tela("home")
    object Carrinho : Tela("carrinho")
    object Pedidos : Tela("pedidos")
    object Perfil : Tela("perfil")
    object Detalhes : Tela("detalhes/{mangaId}") {
        fun criarRota(mangaId: String) = "detalhes/$mangaId"
    }
    object Confirmacao : Tela("confirmacao/{pedidoId}") {
        fun criarRota(pedidoId: String) = "confirmacao/$pedidoId"
    }
}

@Composable
fun NavGraph() {
    val navController: NavHostController = rememberNavController()
    val mangaRepository = MangaRepository()
    val authRepository = AuthRepository()
    val cartViewModel: CartViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Tela.Login.rota
    ) {
        composable(Tela.Login.rota) {
            LoginScreen(
                authViewModel = authViewModel,
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
                authViewModel = authViewModel,
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
                },
                onPedidosClick = {
                    navController.navigate(Tela.Pedidos.rota)
                },
                onPerfilClick = {
                    navController.navigate(Tela.Perfil.rota)
                },
                onLogoutClick = {
                    authViewModel.logout()
                    cartViewModel.limparCarrinho()
                    navController.navigate(Tela.Login.rota) {
                        popUpTo(Tela.Home.rota) { inclusive = true }
                    }
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
                onCompraFinalizada = { pedidoId ->
                    navController.navigate(Tela.Confirmacao.criarRota(pedidoId)) {
                        popUpTo(Tela.Home.rota)
                    }
                }
            )
        }

        composable(
            route = Tela.Confirmacao.rota,
            arguments = listOf(navArgument("pedidoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pedidoId = backStackEntry.arguments?.getString("pedidoId") ?: ""

            ConfirmacaoScreen(
                pedidoId = pedidoId,
                onVoltarParaHome = {
                    navController.navigate(Tela.Home.rota) {
                        popUpTo(Tela.Home.rota) { inclusive = true }
                    }
                },
                onVerPedidos = {
                    navController.navigate(Tela.Pedidos.rota) {
                        popUpTo(Tela.Home.rota)
                    }
                }
            )
        }

        composable(Tela.Pedidos.rota) {
            PedidosScreen(
                usuarioId = authRepository.currentUser?.uid.orEmpty(),
                onVoltar = { navController.popBackStack() }
            )
        }

        composable(Tela.Perfil.rota) {
            ProfileScreen(
                authViewModel = authViewModel,
                onVoltar = { navController.popBackStack() }
            )
        }
    }
}