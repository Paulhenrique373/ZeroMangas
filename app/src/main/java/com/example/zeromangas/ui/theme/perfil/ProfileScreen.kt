package com.example.zeromangas.ui.theme.perfil

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.VermelhoErro
import com.example.zeromangas.viewmodel.AuthViewModel
import com.example.zeromangas.viewmodel.UploadFotoState

/** Central da conta. Upload, edição e sessão seguem sendo tratados pelo AuthViewModel. */
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onVoltar: () -> Unit,
    onPedidosClick: () -> Unit = {},
    onFavoritosClick: () -> Unit = {},
    onEditarPerfilClick: () -> Unit = {},
    onEnderecosClick: () -> Unit = {},
    onNotificacoesClick: () -> Unit = {},
    onCuponsClick: () -> Unit = {},
    onAdminClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val usuario by authViewModel.usuarioAtual.collectAsState()
    val uploadFotoState by authViewModel.uploadFotoState.collectAsState()
    val souAdmin by authViewModel.souAdmin.collectAsState()
    var fotoLocal by remember { mutableStateOf<Uri?>(null) }
    var confirmarSaida by remember { mutableStateOf(false) }

    val seletorImagem = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            fotoLocal = it
            authViewModel.uploadFotoPerfil(context, it)
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.carregarUsuario()
        // Reconfirma no banco toda vez que a tela de Perfil abre — nunca reaproveita
        // um valor antigo, então mesmo que o perfil admin tenha sido revogado depois
        // do último login, o botão do Painel some na próxima vez que o usuário
        // passar por aqui.
        authViewModel.verificarAdmin()
    }
    LaunchedEffect(uploadFotoState) {
        val estado = uploadFotoState
        if (estado is UploadFotoState.Sucesso && !usuario?.nome.isNullOrBlank()) {
            // Persiste a nova URL usando a mesma atualização de perfil já existente.
            authViewModel.atualizarPerfil(usuario?.nome.orEmpty(), estado.url)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Perfil",
            style = MaterialTheme.typography.headlineMedium,
            color = TextoPrincipal,
            modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg)
        )

        Surface(
            color = FundoCard,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal)
        ) {
            Row(
                modifier = Modifier.padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(76.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { seletorImagem.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val imagem = fotoLocal ?: usuario?.fotoUrl?.takeIf { it.isNotBlank() }
                    if (imagem != null) {
                        AsyncImage(imagem, "Foto de perfil", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Default.AccountCircle, "Foto de perfil", tint = TextoSecundario, modifier = Modifier.size(60.dp))
                    }
                    if (uploadFotoState is UploadFotoState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Surface(
                            color = RoxoNeon,
                            shape = CircleShape,
                            modifier = Modifier.align(Alignment.BottomEnd).size(28.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, "Trocar foto", tint = Color.White, modifier = Modifier.padding(6.dp))
                        }
                    }
                }
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(usuario?.nome?.ifBlank { "Sua conta" } ?: "Sua conta", style = MaterialTheme.typography.titleMedium, color = TextoPrincipal)
                    Text(usuario?.email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = TextoSecundario, maxLines = 1)
                    TextButton(onClick = onEditarPerfilClick, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(Spacing.iconSmall))
                        Spacer(Modifier.width(Spacing.xs))
                        Text("Editar perfil")
                    }
                }
            }
        }

        if (uploadFotoState is UploadFotoState.Erro) {
            Text(
                text = (uploadFotoState as UploadFotoState.Erro).mensagem,
                style = MaterialTheme.typography.bodySmall,
                color = VermelhoErro,
                modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm)
            )
        }

        Spacer(Modifier.height(Spacing.sectionGap))
        PerfilSecao("Compras") {
            ItemMenuPerfil(Icons.Default.Receipt, "Meus pedidos", "Acompanhe suas compras", onPedidosClick)
        }
        PerfilSecao("Biblioteca") {
            ItemMenuPerfil(Icons.Default.Favorite, "Favoritos", "Seus mangás salvos", onFavoritosClick)
        }
        PerfilSecao("Entrega") {
            ItemMenuPerfil(Icons.Default.LocationOn, "Meus endereços", "Gerencie onde receber seus pedidos", onEnderecosClick)
        }
        // Só aparece pra quem o banco confirma como admin (souAdmin vem de
        // AuthViewModel.verificarAdmin(), nunca de um valor fixo no app) — cliente
        // comum nunca vê essa seção, nem sabe que ela existe.
        if (souAdmin) {
            PerfilSecao("Administração") {
                ItemMenuPerfil(Icons.Default.AdminPanelSettings, "Painel Administrativo", "Gerenciar loja", onAdminClick)
            }
        }
        PerfilSecao("Conta") {
            ItemMenuPerfil(Icons.Default.Notifications, "Notificações", "Atualizações e novidades", onNotificacoesClick)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ItemMenuPerfil(Icons.Default.LocalOffer, "Cupons", "Aplicar no carrinho", onCuponsClick)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ItemMenuPerfil(Icons.Default.Settings, "Configurações", "Dados e segurança da conta", onEditarPerfilClick)
        }

        Spacer(Modifier.height(Spacing.sm))
        Surface(
            color = FundoCard,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal)
        ) {
            ItemMenuPerfil(
                icone = Icons.Default.Logout,
                titulo = "Sair da conta",
                descricao = null,
                onClick = { confirmarSaida = true },
                cor = VermelhoErro,
                mostrarSeta = false
            )
        }
        Spacer(Modifier.height(Spacing.xl))
    }

    if (confirmarSaida) {
        AlertDialog(
            onDismissRequest = { confirmarSaida = false },
            title = { Text("Sair da conta") },
            text = { Text("Você precisará entrar novamente para acessar sua conta.") },
            confirmButton = { TextButton(onClick = { confirmarSaida = false; onLogoutClick() }) { Text("Sair", color = VermelhoErro) } },
            dismissButton = { TextButton(onClick = { confirmarSaida = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun PerfilSecao(titulo: String, conteudo: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm)) {
        Text(titulo.uppercase(), style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
        Spacer(Modifier.height(Spacing.sm))
        Surface(color = FundoCard, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Column(content = conteudo)
        }
    }
}

@Composable
private fun ItemMenuPerfil(
    icone: ImageVector,
    titulo: String,
    descricao: String?,
    onClick: () -> Unit,
    cor: Color = TextoPrincipal,
    mostrarSeta: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.size(40.dp)) {
            Icon(icone, null, tint = if (cor == VermelhoErro) VermelhoErro else MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
        }
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge, color = cor)
            descricao?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = TextoSecundario) }
        }
        if (mostrarSeta) Icon(Icons.Default.ChevronRight, null, tint = TextoSecundario)
    }
}