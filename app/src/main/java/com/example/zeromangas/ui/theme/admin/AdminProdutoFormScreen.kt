package com.example.zeromangas.ui.theme.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.zeromangas.data.model.Manga
import com.example.zeromangas.data.remote.CategoriaDto
import com.example.zeromangas.data.remote.MarcaDto
import com.example.zeromangas.ui.components.PrimaryButton
import com.example.zeromangas.ui.theme.FundoCard
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoPrincipal
import com.example.zeromangas.ui.theme.VermelhoErro
import com.example.zeromangas.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

/**
 * Formulário de criar/editar produto (item 2 do pedido original).
 * [produtoExistente] nulo = modo "criar"; não nulo = modo "editar" com os campos
 * pré-preenchidos. Marca/categoria são dropdowns porque o banco espera o FK
 * (marca_id/id_categoria), não o texto digitado.
 */
@Composable
fun AdminProdutoFormScreen(
    adminViewModel: AdminViewModel,
    produtoExistente: Manga?,
    onVoltar: () -> Unit
) {
    val categorias by adminViewModel.categorias.collectAsState()
    val marcas by adminViewModel.marcas.collectAsState()
    val escopo = rememberCoroutineScope()

    LaunchedEffect(Unit) { adminViewModel.carregarOpcoesFormulario() }

    var nome by remember { mutableStateOf(produtoExistente?.nome ?: "") }
    var autor by remember { mutableStateOf(produtoExistente?.autor ?: "") }
    var categoriaSelecionada by remember {
        mutableStateOf(produtoExistente?.let { CategoriaDto(id = it.categoriaId, nome = it.categoria) })
    }
    var marcaSelecionada by remember {
        mutableStateOf(produtoExistente?.let { MarcaDto(id = it.marcaId, nome = it.marca) })
    }
    var volume by remember { mutableStateOf((produtoExistente?.volume ?: 1).toString()) }
    var preco by remember { mutableStateOf(produtoExistente?.preco?.toString() ?: "") }
    var estoque by remember { mutableStateOf((produtoExistente?.estoque ?: 0).toString()) }
    var imagemUrl by remember { mutableStateOf(produtoExistente?.imagemUrl ?: "") }
    var descricao by remember { mutableStateOf(produtoExistente?.descricao ?: "") }
    var emDestaque by remember { mutableStateOf(produtoExistente?.emDestaque ?: false) }

    var salvando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    val formularioValido = nome.isNotBlank() && categoriaSelecionada != null && marcaSelecionada != null &&
            preco.toDoubleOrNull() != null && volume.toIntOrNull() != null && estoque.toIntOrNull() != null

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.screenHorizontal),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoltar) {
                Icon(Icons.Default.ArrowBack, "Voltar", tint = TextoPrincipal)
            }
            Spacer(Modifier.width(Spacing.sm))
            Text(
                if (produtoExistente == null) "Novo produto" else "Editar produto",
                style = MaterialTheme.typography.headlineSmall,
                color = TextoPrincipal
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screenHorizontal)
        ) {
            Surface(color = FundoCard, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text("Nome") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Spacing.sm))

                    OutlinedTextField(
                        value = autor,
                        onValueChange = { autor = it },
                        label = { Text("Autor (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Spacing.sm))

                    DropdownSelecao(
                        label = "Categoria",
                        opcoes = categorias,
                        opcaoSelecionada = categoriaSelecionada,
                        nomeOpcao = { it.nome },
                        onSelecionar = { categoriaSelecionada = it }
                    )
                    Spacer(Modifier.height(Spacing.sm))

                    DropdownSelecao(
                        label = "Marca/Editora",
                        opcoes = marcas,
                        opcaoSelecionada = marcaSelecionada,
                        nomeOpcao = { it.nome },
                        onSelecionar = { marcaSelecionada = it }
                    )
                    Spacer(Modifier.height(Spacing.sm))

                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        OutlinedTextField(
                            value = volume,
                            onValueChange = { if (it.length <= 4) volume = it.filter(Char::isDigit) },
                            label = { Text("Volume") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = estoque,
                            onValueChange = { if (it.length <= 6) estoque = it.filter(Char::isDigit) },
                            label = { Text("Estoque") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(Spacing.sm))

                    OutlinedTextField(
                        value = preco,
                        onValueChange = { novo -> if (novo.all { it.isDigit() || it == '.' }) preco = novo },
                        label = { Text("Preço (ex: 39.90)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Spacing.sm))

                    OutlinedTextField(
                        value = imagemUrl,
                        onValueChange = { imagemUrl = it },
                        label = { Text("URL da imagem") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Spacing.sm))

                    OutlinedTextField(
                        value = descricao,
                        onValueChange = { descricao = it },
                        label = { Text("Descrição") },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Spacing.sm))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Em destaque", color = TextoPrincipal, modifier = Modifier.weight(1f))
                        Switch(checked = emDestaque, onCheckedChange = { emDestaque = it })
                    }
                }
            }

            if (erro != null) {
                Spacer(Modifier.height(Spacing.sm))
                Text(erro!!, color = VermelhoErro, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(Spacing.lg))

            PrimaryButton(
                text = "Salvar produto",
                onClick = {
                    erro = null
                    salvando = true
                    escopo.launch {
                        val resultado = adminViewModel.salvarProduto(
                            idProduto = produtoExistente?.id,
                            nome = nome.trim(),
                            marcaId = marcaSelecionada!!.id,
                            categoriaId = categoriaSelecionada!!.id,
                            volume = volume.toIntOrNull() ?: 1,
                            preco = preco.toDoubleOrNull() ?: 0.0,
                            imagemUrl = imagemUrl.trim(),
                            descricao = descricao.trim(),
                            estoque = estoque.toIntOrNull() ?: 0,
                            autor = autor.trim(),
                            emDestaque = emDestaque
                        )
                        salvando = false
                        resultado.onSuccess { onVoltar() }
                            .onFailure { erro = it.message ?: "Não foi possível salvar o produto." }
                    }
                },
                enabled = formularioValido,
                loading = salvando,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun <T> DropdownSelecao(
    label: String,
    opcoes: List<T>,
    opcaoSelecionada: T?,
    nomeOpcao: (T) -> String,
    onSelecionar: (T) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = opcaoSelecionada?.let(nomeOpcao) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ExpandMore, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        // Camada transparente por cima pra capturar o clique (mesmo truque do
        // seletor de gênero em EditarPerfilScreen — TextField readOnly intercepta o toque).
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expandido = true }
        )
        DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            if (opcoes.isEmpty()) {
                DropdownMenuItem(text = { Text("Nenhuma opção cadastrada") }, onClick = {}, enabled = false)
            }
            opcoes.forEach { opcao ->
                DropdownMenuItem(
                    text = { Text(nomeOpcao(opcao)) },
                    onClick = {
                        onSelecionar(opcao)
                        expandido = false
                    }
                )
            }
        }
    }
}