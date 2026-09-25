package com.example.zeromangas.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.zeromangas.ui.components.EmptyState
import com.example.zeromangas.ui.components.LoadingState
import com.example.zeromangas.ui.components.RequerLoginDialog
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.TextoPrincipal
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.repository.AdminRepository
import com.example.zeromangas.repository.AuthRepository
import com.example.zeromangas.repository.MangaRepository
import com.example.zeromangas.ui.theme.admin.AdminDashboardScreen
import com.example.zeromangas.ui.theme.busca.BuscaScreen
import com.example.zeromangas.ui.components.AbaPrincipal
import com.example.zeromangas.ui.components.BottomNavBar
import com.example.zeromangas.ui.theme.detalhes.DetalhesScreen
import com.example.zeromangas.ui.theme.favoritos.FavoritosScreen
import com.example.zeromangas.ui.theme.home.HomeScreen
import com.example.zeromangas.ui.theme.login.LoginScreen
import com.example.zeromangas.ui.theme.register.RegisterScreen
import com.example.zeromangas.ui.theme.cart.CartScreen
import com.example.zeromangas.ui.theme.checkout.CheckoutScreen
import com.example.zeromangas.ui.theme.confirmacao.ConfirmacaoScreen
import com.example.zeromangas.ui.theme.pedidos.PedidosScreen
import com.example.zeromangas.ui.theme.perfil.ProfileScreen
import com.example.zeromangas.ui.theme.editarperfil.EditarPerfilScreen
import com.example.zeromangas.ui.theme.enderecos.EnderecosScreen
import com.example.zeromangas.ui.theme.notificacoes.NotificacoesScreen
import com.example.zeromangas.viewmodel.AuthViewModel
import com.example.zeromangas.viewmodel.AvaliacaoViewModel
import com.example.zeromangas.viewmodel.CartViewModel
import com.example.zeromangas.viewmodel.EnderecoViewModel
import com.example.zeromangas.viewmodel.FavoritoViewModel
import com.example.zeromangas.viewmodel.HomeViewModel
import com.example.zeromangas.viewmodel.NotificacaoViewModel
import kotlinx.coroutines.launch

sealed class Tela(val rota: String) {
    object Login : Tela("login")
    object Cadastro : Tela("cadastro")
    object Home : Tela("home")
    object Carrinho : Tela("carrinho")
    object Checkout : Tela("checkout")
    object Pedidos : Tela("pedidos")
    object Perfil : Tela("perfil")
    object EditarPerfil : Tela("editar_perfil")
    object Enderecos : Tela("enderecos")
    object Favoritos : Tela("favoritos")
    object Busca : Tela("busca")
    object Notificacoes : Tela("notificacoes")
    object Admin : Tela("admin")
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
    val adminRepository = remember { AdminRepository() }
    val authRepository = AuthRepository()
    val cartViewModel: CartViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val favoritoViewModel: FavoritoViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val notificacaoViewModel: NotificacaoViewModel = viewModel()

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
    val escopoNav = rememberCoroutineScope()

    // ---- Modo visitante ----
    // Não existe um "estado de visitante" separado: um usuário sem conta é
    // simplesmente authRepository.currentUser == null, exatamente como o resto
    // do código já trata (authRepository.currentUser?.uid.orEmpty() em cada
    // tela). "Continuar sem conta" só pula a tela de Login sem criar sessão
    // nenhuma no Supabase Auth (item 8 do pedido).
    //
    // rotaPendenteAposLogin guarda pra onde navegar quando o visitante conclui
    // login/cadastro DEPOIS de ter sido barrado tentando usar algo que exige
    // conta (compra, perfil, favoritos, etc). Null = fluxo normal de
    // login/cadastro, vai pra Home como sempre foi.
    var rotaPendenteAposLogin by remember { mutableStateOf<String?>(null) }
    var mostrarDialogoLogin by remember { mutableStateOf(false) }

    fun exigirLogin(destinoAposLogin: String?) {
        rotaPendenteAposLogin = destinoAposLogin
        mostrarDialogoLogin = true
    }

    fun navegarAposAutenticar() {
        val destino = rotaPendenteAposLogin
        rotaPendenteAposLogin = null
        navController.navigate(destino ?: Tela.Home.rota) {
            popUpTo(Tela.Login.rota) { inclusive = true }
        }
    }

    // ETAPA 11 (polimento, parte 3): Snackbar global de sucesso, vivendo no NavGraph
    // (fora de qualquer tela específica) pra funcionar não importa de onde o item
    // tenha sido adicionado ao carrinho — Home, Detalhes ou Favoritos.
    val snackbarHostState = remember { SnackbarHostState() }
    val mensagemSucesso by cartViewModel.mensagemSucesso.collectAsState()

    LaunchedEffect(mensagemSucesso) {
        val mensagem = mensagemSucesso
        if (mensagem != null) {
            snackbarHostState.showSnackbar(mensagem)
            cartViewModel.limparMensagemSucesso()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { dados ->
                Snackbar(
                    containerColor = FundoCard,
                    contentColor = TextoPrincipal,
                    actionContentColor = RoxoNeon
                ) {
                    androidx.compose.material3.Text("✓ ${dados.visuals.message}")
                }
            }
        },
        bottomBar = {
            if (rotaAtual in rotasComBottomBar) {
                BottomNavBar(
                    rotaAtual = rotaAtual,
                    quantidadeNoCarrinho = quantidadeNoCarrinho,
                    onAbaSelecionada = { aba ->
                        if (aba.rota != rotaAtual) {
                            // Item 7: Favoritos e Perfil dependem de uma conta (favoritos
                            // vinculados ao usuário, dados pessoais, pedidos, endereços).
                            // Barra o visitante ANTES de navegar, em vez de deixar a tela
                            // abrir vazia/quebrada — Home, Busca e Carrinho continuam livres.
                            val exigeConta = aba == AbaPrincipal.FAVORITOS || aba == AbaPrincipal.PERFIL
                            if (exigeConta && authRepository.currentUser == null) {
                                exigirLogin(aba.rota)
                            } else {
                                navController.navigate(aba.rota) {
                                    popUpTo(Tela.Home.rota) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { paddingInterno ->
        if (mostrarDialogoLogin) {
            RequerLoginDialog(
                onEntrar = {
                    mostrarDialogoLogin = false
                    navController.navigate(Tela.Login.rota)
                },
                onCriarConta = {
                    mostrarDialogoLogin = false
                    // Mesmo caminho Login -> Cadastro que o app já usa (Cadastro fica
                    // empilhado sobre Login), pra o popUpTo(Login) do navegarAposAutenticar()
                    // continuar limpando as duas telas depois do cadastro concluído.
                    navController.navigate(Tela.Login.rota)
                    navController.navigate(Tela.Cadastro.rota)
                },
                onContinuarNavegando = {
                    mostrarDialogoLogin = false
                    rotaPendenteAposLogin = null
                }
            )
        }

        NavHost(
            navController = navController,
            startDestination = Tela.Login.rota,
            modifier = Modifier.padding(paddingInterno),
            // ETAPA 11 (polimento): transição de fade suave entre TODAS as telas
            // (padrão global do NavHost, nenhuma tela precisou ser alterada pra ganhar isso).
            // Optou-se por fade em vez de slide direcional porque o mesmo NavHost também
            // atende a troca de abas do BottomNavBar, onde um slide de "avançar/voltar"
            // não faz sentido semântico.
            enterTransition = { fadeIn(animationSpec = tween(220)) },
            exitTransition = { fadeOut(animationSpec = tween(180)) },
            popEnterTransition = { fadeIn(animationSpec = tween(220)) },
            popExitTransition = { fadeOut(animationSpec = tween(180)) }
        ) {
            composable(Tela.Login.rota) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoginSucesso = { navegarAposAutenticar() },
                    onIrParaCadastro = {
                        navController.navigate(Tela.Cadastro.rota)
                    },
                    onContinuarSemConta = {
                        // Item 8: não cria conta nem sessão nenhuma no Supabase Auth.
                        // Antes de entrar como visitante, garante (suspend, então
                        // esperamos terminar) que nenhuma sessão antiga salva no
                        // aparelho continua valendo — senão o app "reconheceria"
                        // o visitante como o usuário anterior (e-mail antigo
                        // aparecendo em qualquer tela que leia currentUser).
                        rotaPendenteAposLogin = null
                        escopoNav.launch {
                            authRepository.encerrarSessaoResidual()
                            navController.navigate(Tela.Home.rota) {
                                popUpTo(Tela.Login.rota) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Tela.Cadastro.rota) {
                RegisterScreen(
                    authViewModel = authViewModel,
                    onCadastroSucesso = { navegarAposAutenticar() },
                    onVoltarParaLogin = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Tela.Home.rota) {
                HomeScreen(
                    homeViewModel = homeViewModel,
                    favoritoViewModel = favoritoViewModel,
                    notificacaoViewModel = notificacaoViewModel,
                    usuarioId = authRepository.currentUser?.uid.orEmpty(),
                    onMangaClick = { manga ->
                        navController.navigate(Tela.Detalhes.criarRota(manga.id))
                    },
                    onNotificacoesClick = {
                        // Notificações são vinculadas à conta (item 7).
                        if (authRepository.currentUser == null) {
                            exigirLogin(Tela.Notificacoes.rota)
                        } else {
                            navController.navigate(Tela.Notificacoes.rota)
                        }
                    },
                    onRequerLogin = { exigirLogin(null) },
                    quantidadeNoCarrinho = quantidadeNoCarrinho,
                    onCarrinhoClick = {
                        navController.navigate(Tela.Carrinho.rota) {
                            popUpTo(Tela.Home.rota) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onBuscaClick = {
                        navController.navigate(Tela.Busca.rota) {
                            popUpTo(Tela.Home.rota) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
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
                    val resultado = mangaRepository.buscarMangaComRecomendados(mangaId)
                    resultado.fold(
                        onSuccess = { (mangaEncontrado, recomendadosEncontrados) ->
                            manga = mangaEncontrado
                            recomendados = recomendadosEncontrados
                        },
                        onFailure = {
                            erro = "Não foi possível carregar o mangá."
                        }
                    )
                    carregando = false
                }

                when {
                    carregando -> {
                        LoadingState(modifier = Modifier.fillMaxSize())
                    }
                    erro != null -> {
                        EmptyState(
                            titulo = "Não foi possível carregar",
                            subtitulo = erro,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        val avaliacaoViewModel: AvaliacaoViewModel = viewModel()
                        DetalhesScreen(
                            manga = manga,
                            recomendados = recomendados,
                            favoritoViewModel = favoritoViewModel,
                            usuarioId = authRepository.currentUser?.uid.orEmpty(),
                            onRequerLogin = { exigirLogin(null) },
                            onVoltar = { navController.popBackStack() },
                            onAdicionarAoCarrinho = { mangaSelecionado, quantidade ->
                                repeat(quantidade) { cartViewModel.adicionarItem(mangaSelecionado) }
                                navController.popBackStack()
                            },
                            onMangaClick = { mangaSelecionado ->
                                navController.navigate(Tela.Detalhes.criarRota(mangaSelecionado.id))
                            },
                            avaliacaoViewModel = avaliacaoViewModel
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
                        // Regra principal (item 2/10): navegar não exige conta, comprar exige.
                        // O carrinho em si não é mexido aqui — continua intacto no CartViewModel
                        // enquanto o visitante entra/cadastra e volta pra cá (item 4).
                        if (authRepository.currentUser == null) {
                            exigirLogin(Tela.Checkout.rota)
                        } else {
                            navController.navigate(Tela.Checkout.rota)
                        }
                    },
                    onExplorarClick = {
                        // Mesmo padrão de troca de aba usado pelo BottomNavBar,
                        // pra não empilhar telas duplicadas no back stack.
                        navController.navigate(Tela.Home.rota) {
                            popUpTo(Tela.Home.rota)
                            launchSingleTop = true
                        }
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
                    onVoltar = { navController.popBackStack() },
                    onExplorarClick = {
                        navController.navigate(Tela.Home.rota) {
                            popUpTo(Tela.Home.rota)
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Tela.Perfil.rota) {
                ProfileScreen(
                    authViewModel = authViewModel,
                    onVoltar = { navController.popBackStack() },
                    onPedidosClick = { navController.navigate(Tela.Pedidos.rota) },
                    onFavoritosClick = {
                        navController.navigate(Tela.Favoritos.rota) {
                            popUpTo(Tela.Home.rota)
                            launchSingleTop = true
                        }
                    },
                    onEditarPerfilClick = {
                        navController.navigate(Tela.EditarPerfil.rota)
                    },
                    onEnderecosClick = {
                        navController.navigate(Tela.Enderecos.rota)
                    },
                    onNotificacoesClick = {
                        navController.navigate(Tela.Notificacoes.rota)
                    },
                    onCuponsClick = {
                        navController.navigate(Tela.Carrinho.rota) {
                            popUpTo(Tela.Home.rota)
                            launchSingleTop = true
                        }
                    },
                    onAdminClick = {
                        navController.navigate(Tela.Admin.rota)
                    },
                    onLogoutClick = {
                        authViewModel.logout()
                        cartViewModel.limparCarrinho()
                        favoritoViewModel.limparFavoritos()
                        notificacaoViewModel.limpar()
                        navController.navigate(Tela.Login.rota) {
                            popUpTo(Tela.Home.rota) { inclusive = true }
                        }
                    }
                )
            }

            composable(Tela.EditarPerfil.rota) {
                EditarPerfilScreen(
                    authViewModel = authViewModel,
                    onVoltar = { navController.popBackStack() }
                )
            }

            composable(Tela.Enderecos.rota) {
                val enderecoViewModel: EnderecoViewModel = viewModel()
                EnderecosScreen(
                    enderecoViewModel = enderecoViewModel,
                    onVoltar = { navController.popBackStack() }
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
                    },
                    onExplorarClick = {
                        navController.navigate(Tela.Home.rota) {
                            popUpTo(Tela.Home.rota)
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Tela.Notificacoes.rota) {
                NotificacoesScreen(
                    notificacaoViewModel = notificacaoViewModel,
                    usuarioId = authRepository.currentUser?.uid.orEmpty(),
                    onVoltar = { navController.popBackStack() },
                    onNotificacaoClick = { notificacao ->
                        // Leva pra origem da notificação quando existir: produto (promoção,
                        // lançamento, favorito voltou ao estoque/entrou em promoção) ou
                        // pedido (atualização de status). Notificação sem nenhum dos dois
                        // (ex: aviso genérico) só marca como lida e fica na própria tela.
                        val produtoId = notificacao.produtoId
                        val pedidoId = notificacao.pedidoId
                        when {
                            produtoId != null -> navController.navigate(Tela.Detalhes.criarRota(produtoId))
                            pedidoId != null -> navController.navigate(Tela.Pedidos.rota)
                        }
                    }
                )
            }

            composable(Tela.Admin.rota) {
                // Item 33 do pedido: proteção contra acesso direto. Mesmo que alguém
                // force a navegação pra "admin" (deep link, back stack manipulado etc),
                // essa tela sempre reconfirma no banco antes de mostrar qualquer coisa —
                // nunca reaproveita um estado do AuthViewModel só porque a navegação
                // partiu do botão do Perfil. Nenhum dado administrativo é carregado
                // antes dessa confirmação.
                var verificando by remember { mutableStateOf(true) }
                var autorizado by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    verificando = true
                    autorizado = adminRepository.souAdmin().getOrDefault(false)
                    verificando = false
                }

                when {
                    verificando -> LoadingState(modifier = Modifier.fillMaxSize())
                    autorizado -> AdminDashboardScreen(onVoltar = { navController.popBackStack() })
                    else -> EmptyState(
                        titulo = "Acesso negado",
                        subtitulo = "Você não tem permissão para acessar o painel administrativo.",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            composable(Tela.Busca.rota) {
                BuscaScreen(
                    buscaViewModel = homeViewModel,
                    favoritoViewModel = favoritoViewModel,
                    usuarioId = authRepository.currentUser?.uid.orEmpty(),
                    onRequerLogin = { exigirLogin(null) },
                    onMangaClick = { manga ->
                        navController.navigate(Tela.Detalhes.criarRota(manga.id))
                    }
                )
            }
        }
    }
}