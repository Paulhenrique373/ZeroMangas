package com.example.zeromangas.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.repository.AuthRepository
import com.example.zeromangas.repository.MangaRepository
import com.example.zeromangas.ui.busca.BuscaScreen
import com.example.zeromangas.ui.components.AbaPrincipal
import com.example.zeromangas.ui.components.BottomNavBar
import com.example.zeromangas.ui.detalhes.DetalhesScreen
import com.example.zeromangas.ui.favoritos.FavoritosScreen
import com.example.zeromangas.ui.home.HomeScreen
import com.example.zeromangas.ui.login.LoginScreen
import com.example.zeromangas.ui.register.RegisterScreen
import com.example.zeromangas.ui.theme.cart.CartScreen
import com.example.zeromangas.ui.theme.checkout.CheckoutScreen
import com.example.zeromangas.ui.theme.confirmacao.ConfirmacaoScreen
import com.example.zeromangas.ui.theme.pedidos.PedidosScreen
import com.example.zeromangas.ui.perfil.ProfileScreen
import com.example.zeromangas.viewmodel.AuthViewModel
import com.example.zeromangas.viewmodel.CartViewModel
import com.example.zeromangas.viewmodel.FavoritoViewModel

sealed class Tela(val rota: String) {
    object Login : Tela("login")
    object Cadastro : Tela("cadastro")
    object Home : Tela("home")
    object Carrinho : Tela("carrinho")
    object Checkout : Tela("checkout")
    object Pedidos : Tela("pedidos")
    object Perfil : Tela("perfil")
    object Favoritos : Tela("favoritos")
    object Busca : Tela("busca")
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
    val mangaRepository = remember { MangaRepository() }
    val authRepository = AuthRepository()
    val cartViewModel: CartViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val favoritoViewModel: FavoritoViewModel = viewModel()

    // Rotas em que a navegação inferior deve aparecer.
    val rotasComBottomBar = setOf(
        Tela.Home.rota,
        Tela.Busca.rota,
        Tela.Carrinho.rota,
        Tela.Favoritos.rota,
        Tela.Perfil.rota
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val rotaAtual = backStackEntry?.destination?.route
    val itensCarrinho by cartViewModel.itens.collectAsState()
    val quantidadeNoCarrinho = itensCarrinho.sumOf { it.quantidade }

    Scaffold(
        bottomBar = {
            if (rotaAtual in rotasComBottomBar) {
                BottomNavBar(
                    rotaAtual = rotaAtual,
                    quantidadeNoCarrinho = quantidadeNoCarrinho,
                    onAbaSelecionada = { aba ->
                        if (aba.rota != rotaAtual) {
                            navController.navigate(aba.rota) {
                                popUpTo(Tela.Home.rota)
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    ) { paddingInterno ->
        NavHost(
            navController = navController,
            startDestination = Tela.Login.rota,
            modifier = Modifier.padding(paddingInterno)
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
                    favoritoViewModel = favoritoViewModel,
                    usuarioId = authRepository.currentUser?.uid.orEmpty(),
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
                    onFavoritosClick = {
                        navController.navigate(Tela.Favoritos.rota)
                    },
                    onLogoutClick = {
                        authViewModel.logout()
                        cartViewModel.limparCarrinho()
                        favoritoViewModel.limparFavoritos()
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

                var manga by remember { mutableStateOf<Manga?>(null) }
                var recomendados by remember { mutableStateOf<List<Manga>>(emptyList()) }
                var carregando by remember { mutableStateOf(true) }
                var erro by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(mangaId) {
                    carregando = true
                    erro = null
                    val resultado = mangaRepository.listarMangas()
                    resultado.fold(
                        onSuccess = { lista ->
                            val encontrado = lista.find { it.id == mangaId }
                            manga = encontrado
                            recomendados = if (encontrado != null) {
                                lista.filter { it.categoria == encontrado.categoria && it.id != encontrado.id }
                                    .take(10)
                            } else {
                                emptyList()
                            }
                        },
                        onFailure = {
                            erro = "Não foi possível carregar o mangá."
                        }
                    )
                    carregando = false
                }

                when {
                    carregando -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    erro != null -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(text = erro ?: "", modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    else -> {
                        DetalhesScreen(
                            manga = manga,
                            recomendados = recomendados,
                            favoritoViewModel = favoritoViewModel,
                            usuarioId = authRepository.currentUser?.uid.orEmpty(),
                            onVoltar = { navController.popBackStack() },
                            onAdicionarAoCarrinho = { mangaSelecionado ->
                                cartViewModel.adicionarItem(mangaSelecionado)
                                navController.popBackStack()
                            },
                            onMangaClick = { mangaSelecionado ->
                                navController.navigate(Tela.Detalhes.criarRota(mangaSelecionado.id))
                            }
                        )
                    }
                }
            }

            composable(Tela.Carrinho.rota) {
                CartScreen(
                    cartViewModel = cartViewModel,
                    usuarioId = authRepository.currentUser?.uid.orEmpty(),
                    onVoltar = { navController.popBackStack() },
                    onIrParaCheckout = {
                        navController.navigate(Tela.Checkout.rota)
                    }
                )
            }

            composable(Tela.Checkout.rota) {
                CheckoutScreen(
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
                    onVoltar = { navController.popBackStack() },
                    onPedidosClick = { navController.navigate(Tela.Pedidos.rota) },
                    onFavoritosClick = { navController.navigate(Tela.Favoritos.rota) },
                    onLogoutClick = {
                        authViewModel.logout()
                        cartViewModel.limparCarrinho()
                        favoritoViewModel.limparFavoritos()
                        navController.navigate(Tela.Login.rota) {
                            popUpTo(Tela.Home.rota) { inclusive = true }
                        }
                    }
                )
            }

            composable(Tela.Favoritos.rota) {
                FavoritosScreen(
                    favoritoViewModel = favoritoViewModel,
                    cartViewModel = cartViewModel,
                    usuarioId = authRepository.currentUser?.uid.orEmpty(),
                    onVoltar = { navController.popBackStack() },
                    onMangaClick = { manga ->
                        navController.navigate(Tela.Detalhes.criarRota(manga.id))
                    }
                )
            }

            composable(Tela.Busca.rota) {
                BuscaScreen(
                    favoritoViewModel = favoritoViewModel,
                    usuarioId = authRepository.currentUser?.uid.orEmpty(),
                    onMangaClick = { manga ->
                        navController.navigate(Tela.Detalhes.criarRota(manga.id))
                    }
                )
            }
        }
    }
}