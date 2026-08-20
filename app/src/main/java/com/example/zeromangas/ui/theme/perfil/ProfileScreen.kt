package com.example.zeromangas.ui.perfil

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zeromangas.ui.components.PrimaryButton
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.RoxoNeon
import com.example.zeromangas.ui.theme.RoxoNeonClaro
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.VerdeSucesso
import com.example.zeromangas.ui.theme.VermelhoErro
import com.example.zeromangas.viewmodel.AuthViewModel
import com.example.zeromangas.viewmodel.ProfileState
import com.example.zeromangas.viewmodel.UploadFotoState

/**
 * Tela de perfil. Toda a lógica (carregar usuário, editar nome, trocar foto via
 * Supabase Storage) continua 100% no [AuthViewModel] já existente — só o visual muda,
 * agora com o design system, e o menu ganhou atalhos reais para as telas que já existem
 * (Pedidos, Favoritos, Sair). Não incluí "Endereços", "Cupons", "Configurações" ou
 * "Notificações" do planejamento original porque essas telas ainda não existem no
 * projeto — um atalho pra elas ficaria quebrado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onVoltar: () -> Unit,
    onPedidosClick: () -> Unit = {},
    onFavoritosClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val usuario by authViewModel.usuarioAtual.collectAsState()
    val profileState by authViewModel.profileState.collectAsState()
    val uploadFotoState by authViewModel.uploadFotoState.collectAsState()

    var nome by remember { mutableStateOf("") }
    var fotoUrl by remember { mutableStateOf("") }
    var fotoLocalPreview by remember { mutableStateOf<Uri?>(null) }
    var jaCarregouCampos by remember { mutableStateOf(false) }

    val seletorImagem = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            fotoLocalPreview = uri
            authViewModel.uploadFotoPerfil(context, uri)
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.carregarUsuario()
    }

    LaunchedEffect(usuario) {
        if (!jaCarregouCampos && usuario != null) {
            nome = usuario?.nome.orEmpty()
            fotoUrl = usuario?.fotoUrl.orEmpty()
            jaCarregouCampos = true
        }
    }

    LaunchedEffect(uploadFotoState) {
        val estado = uploadFotoState
        if (estado is UploadFotoState.Sucesso) {
            fotoUrl = estado.url
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoltar) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextoPrincipal)
            }
            Text(
                text = "Meu Perfil",
                style = MaterialTheme.typography.titleLarge,
                color = TextoPrincipal,
                modifier = Modifier.padding(start = Spacing.sm)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(FundoCard)
                    .clickable { seletorImagem.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                val modeloImagem = fotoLocalPreview ?: fotoUrl.ifBlank { null }

                if (modeloImagem != null) {
                    AsyncImage(
                        model = modeloImagem,
                        contentDescription = "Foto de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.size(100.dp),
                        tint = TextoSecundario
                    )
                }

                if (uploadFotoState is UploadFotoState.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = TextoPrincipal
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(RoxoNeon),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Trocar foto",
                            modifier = Modifier.size(18.dp),
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "Toque na foto para escolher uma da galeria",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )

            if (uploadFotoState is UploadFotoState.Erro) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = (uploadFotoState as UploadFotoState.Erro).mensagem,
                    style = MaterialTheme.typography.bodySmall,
                    color = VermelhoErro
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            OutlinedTextField(
                value = usuario?.email.orEmpty(),
                onValueChange = {},
                label = { Text("E-mail") },
                enabled = false,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = "O e-mail não pode ser alterado por aqui.",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )

            when (val estado = profileState) {
                is ProfileState.Erro -> {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = estado.mensagem,
                        style = MaterialTheme.typography.bodySmall,
                        color = VermelhoErro
                    )
                }
                is ProfileState.Sucesso -> {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = "Perfil atualizado com sucesso!",
                        style = MaterialTheme.typography.bodySmall,
                        color = VerdeSucesso
                    )
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            PrimaryButton(
                text = "Salvar alterações",
                onClick = { authViewModel.atualizarPerfil(nome, fotoUrl) },
                enabled = profileState !is ProfileState.Loading && uploadFotoState !is UploadFotoState.Loading,
                loading = profileState is ProfileState.Loading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Spacing.radiusMedium))
                    .background(FundoCard)
            ) {
                ItemMenuPerfil(
                    icone = Icons.Default.Receipt,
                    rotulo = "Meus pedidos",
                    onClick = onPedidosClick
                )
                HorizontalDivider(color = TextoSecundario.copy(alpha = 0.12f))
                ItemMenuPerfil(
                    icone = Icons.Default.Favorite,
                    rotulo = "Meus favoritos",
                    onClick = onFavoritosClick
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Spacing.radiusMedium))
                    .background(FundoCard)
            ) {
                ItemMenuPerfil(
                    icone = Icons.Default.Logout,
                    rotulo = "Sair",
                    corTexto = VermelhoErro,
                    corIcone = VermelhoErro,
                    mostrarSeta = false,
                    onClick = onLogoutClick
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

/** Item de linha do menu do perfil (ícone + rótulo + seta), reutilizado para cada atalho. */
@Composable
private fun ItemMenuPerfil(
    icone: ImageVector,
    rotulo: String,
    onClick: () -> Unit,
    corTexto: androidx.compose.ui.graphics.Color = TextoPrincipal,
    corIcone: androidx.compose.ui.graphics.Color = RoxoNeonClaro,
    mostrarSeta: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icone, contentDescription = null, tint = corIcone)
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodyLarge,
            color = corTexto,
            modifier = Modifier.weight(1f)
        )
        if (mostrarSeta) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextoSecundario
            )
        }
    }
}