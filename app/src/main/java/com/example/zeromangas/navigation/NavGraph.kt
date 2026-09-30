package com.example.zeromangas.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.zeromangas.data.local.VistosRecentemente
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
import com.example.zeromangas.ui.theme.admin.AdminCategoriasMarcasScreen
import com.example.zeromangas.ui.theme.admin.AdminDashboardScreen
import com.example.zeromangas.ui.theme.admin.AdminClientesScreen
import com.example.zeromangas.ui.theme.admin.AdminCuponsScreen
import com.example.zeromangas.ui.theme.admin.AdminEstoqueScreen
import com.example.zeromangas.ui.theme.admin.AdminPedidosScreen
import com.example.zeromangas.ui.theme.admin.AdminProdutoFormScreen
import com.example.zeromangas.ui.theme.admin.AdminProdutosScreen
import com.example.zeromangas.ui.theme.admin.ItemNomeado
import com.example.zeromangas.viewmodel.AdminClientesViewModel
import com.example.zeromangas.viewmodel.AdminCuponsViewModel
import com.example.zeromangas.viewmodel.AdminPedidosViewModel
import com.example.zeromangas.viewmodel.AdminViewModel
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
    object AdminProdutos : Tela("admin_produtos")
    object AdminProdutoForm : Tela("admin_produto_form/{produtoId}") {
        fun criarRota(produtoId: String) = "admin_produto_form/$produtoId"
    }
    object AdminCategorias : Tela("admin_categorias")
    object AdminMarcas : Tela("admin_marcas")
    object AdminEstoque : Tela("admin_estoque")
    object AdminPedidos : Tela("admin_pedidos")
    object AdminClientes : Tela("admin_clientes")
    object AdminCupons : Tela("admin_cupons")
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
    val adminViewModel: AdminViewModel = viewModel()
    val adminPedidosViewModel: AdminPedidosViewModel = viewModel()
    val adminClientesViewModel: AdminClientesViewModel = viewModel()
    val adminCuponsViewModel: AdminCuponsViewModel = viewModel()

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

    // Navega para uma aba da barra inferior. Usado tanto pelo toque no BottomNavBar
    // quanto pelo deslizamento lateral, então as duas formas têm a mesma regra.
    // Favoritos e Perfil dependem de uma conta: visitante vê o diálogo de login.
    fun irParaAba(aba: AbaPrincipal) {
        if (aba.rota == rotaAtual) return
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

    // Deslizar pra esquerda = próxima aba; pra direita = aba anterior.
    val limiteDeslizePx = with(LocalDensity.current) { 80.dp.toPx() }

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
                    onAbaSelecionada = { aba -> irParaAba(aba) }
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingInterno)
                .pointerInput(rotaAtual) {
                    // Só nas 5 telas da barra inferior. Listas horizontais (ex: LazyRow da Home)
                    // consomem o próprio arrasto, então o deslize de aba só vale fora delas.
                    if (rotaAtual !in rotasComBottomBar) return@pointerInput
                    var arrastado = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { arrastado = 0f },
                        onDragCancel = { arrastado = 0f },
                        onDragEnd = {
                            val indice = AbaPrincipal.entries.indexOfFirst { it.rota == rotaAtual }
                            val destino = when {
                                arrastado <= -limiteDeslizePx -> indice + 1
                                arrastado >= limiteDeslizePx -> indice - 1
                                else -> -1
                            }
                            if (indice >= 0 && destino in AbaPrincipal.entries.indices) {
                                irParaAba(AbaPrincipal.entries[destino])
                            }
                            arrastado = 0f
                        },
                        onHorizontalDrag = { _, quanto -> arrastado += quanto }
                    )
                }
        ) {
            NavHost(
                navController = navController,
                startDestination = Tela.Login.rota,
                modifier = Modifier,
                // Troca entre abas desliza na direção do gesto; o resto do app segue no fade.
                enterTransition = {
                    val de = AbaPrincipal.entries.indexOfFirst { it.rota == initialState.destination.route }
                    val para = AbaPrincipal.entries.indexOfFirst { it.rota == targetState.destination.route }
                    if (de >= 0 && para >= 0 && de != para) {
                        slideInHorizontally(tween(260)) { largura -> if (para > de) largura else -largura } +
                                fadeIn(animationSpec = tween(260))
                    } else {
                        fadeIn(animationSpec = tween(220))
                    }
                },
                exitTransition = {
                    val de = AbaPrincipal.entries.indexOfFirst { it.rota == initialState.destination.route }
                    val para = AbaPrincipal.entries.indexOfFirst { it.rota == targetState.destination.route }
                    if (de >= 0 && para >= 0 && de != para) {
                        slideOutHorizontally(tween(260)) { largura -> if (para > de) -largura else largura } +
                                fadeOut(animationSpec = tween(260))
                    } else {
                        fadeOut(animationSpec = tween(180))
                    }
                },
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

                    val contextoDetalhes = LocalContext.current
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
                                mangaEncontrado?.let { VistosRecentemente.registrar(contextoDetalhes, it.id) }
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
                        cartViewModel = cartViewModel,
                        onIrParaCarrinho = { navController.navigate(Tela.Carrinho.rota) },
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
                        autorizado -> AdminDashboardScreen(
                            adminRepository = adminRepository,
                            onVoltar = { navController.popBackStack() },
                            onProdutosClick = { navController.navigate(Tela.AdminProdutos.rota) },
                            onCategoriasClick = { navController.navigate(Tela.AdminCategorias.rota) },
                            onMarcasClick = { navController.navigate(Tela.AdminMarcas.rota) },
                            onEstoqueClick = { navController.navigate(Tela.AdminEstoque.rota) },
                            onPedidosClick = { navController.navigate(Tela.AdminPedidos.rota) },
                            onClientesClick = { navController.navigate(Tela.AdminClientes.rota) },
                            onCuponsClick = { navController.navigate(Tela.AdminCupons.rota) }
                        )
                        else -> EmptyState(
                            titulo = "Acesso negado",
                            subtitulo = "Você não tem permissão para acessar o painel administrativo.",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                composable(Tela.AdminProdutos.rota) {
                    AdminProdutosScreen(
                        adminViewModel = adminViewModel,
                        onVoltar = { navController.popBackStack() },
                        onNovoProdutoClick = { navController.navigate(Tela.AdminProdutoForm.criarRota("novo")) },
                        onEditarProdutoClick = { produto ->
                            navController.navigate(Tela.AdminProdutoForm.criarRota(produto.id))
                        }
                    )
                }

                composable(
                    route = Tela.AdminProdutoForm.rota,
                    arguments = listOf(navArgument("produtoId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val produtoId = backStackEntry.arguments?.getString("produtoId") ?: "novo"
                    val produtoExistente = if (produtoId == "novo") null else adminViewModel.produtoPorId(produtoId)

                    AdminProdutoFormScreen(
                        adminViewModel = adminViewModel,
                        produtoExistente = produtoExistente,
                        onVoltar = { navController.popBackStack() }
                    )
                }

                composable(Tela.AdminCategorias.rota) {
                    val categorias by adminViewModel.categorias.collectAsState()
                    val carregando by adminViewModel.carregandoCategoriasMarcas.collectAsState()
                    AdminCategoriasMarcasScreen(
                        titulo = "Categorias",
                        itens = categorias.map { ItemNomeado(it.id, it.nome) },
                        carregando = carregando,
                        onVoltar = { navController.popBackStack() },
                        onRecarregar = { adminViewModel.carregarCategorias() },
                        onSalvar = { id, nome -> adminViewModel.salvarCategoria(id, nome) }
                    )
                }

                composable(Tela.AdminMarcas.rota) {
                    val marcas by adminViewModel.marcas.collectAsState()
                    val carregando by adminViewModel.carregandoCategoriasMarcas.collectAsState()
                    AdminCategoriasMarcasScreen(
                        titulo = "Editoras",
                        itens = marcas.map { ItemNomeado(it.id, it.nome) },
                        carregando = carregando,
                        onVoltar = { navController.popBackStack() },
                        onRecarregar = { adminViewModel.carregarMarcas() },
                        onSalvar = { id, nome -> adminViewModel.salvarMarca(id, nome) }
                    )
                }

                composable(Tela.AdminEstoque.rota) {
                    AdminEstoqueScreen(
                        adminViewModel = adminViewModel,
                        onVoltar = { navController.popBackStack() }
                    )
                }

                composable(Tela.AdminPedidos.rota) {
                    AdminPedidosScreen(
                        viewModel = adminPedidosViewModel,
                        onVoltar = { navController.popBackStack() }
                    )
                }

                composable(Tela.AdminClientes.rota) {
                    AdminClientesScreen(
                        viewModel = adminClientesViewModel,
                        onVoltar = { navController.popBackStack() }
                    )
                }

                composable(Tela.AdminCupons.rota) {
                    AdminCuponsScreen(
                        viewModel = adminCuponsViewModel,
                        onVoltar = { navController.popBackStack() }
                    )
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
}