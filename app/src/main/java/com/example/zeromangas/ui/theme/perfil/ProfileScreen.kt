package com.example.zeromangas.ui.perfil

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zeromangas.viewmodel.AuthViewModel
import com.example.zeromangas.viewmodel.ProfileState
import com.example.zeromangas.viewmodel.UploadFotoState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onVoltar: () -> Unit,
    onPedidosClick: () -> Unit = {}
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

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoltar) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
            }
            Text(
                text = "Meu Perfil",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uploadFotoState is UploadFotoState.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Trocar foto",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Toque na foto para escolher uma da galeria",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uploadFotoState is UploadFotoState.Erro) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = (uploadFotoState as UploadFotoState.Erro).mensagem,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = usuario?.email.orEmpty(),
                onValueChange = {},
                label = { Text("E-mail") },
                enabled = false,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "O e-mail não pode ser alterado por aqui.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (val estado = profileState) {
                is ProfileState.Erro -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = estado.mensagem,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is ProfileState.Sucesso -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Perfil atualizado com sucesso!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { authViewModel.atualizarPerfil(nome, fotoUrl) },
                enabled = profileState !is ProfileState.Loading && uploadFotoState !is UploadFotoState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (profileState is ProfileState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Salvar alterações")
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Divider(color = MaterialTheme.colorScheme.surface)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPedidosClick() }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Meus pedidos",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}