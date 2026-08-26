package com.example.zeromangas.ui.theme.enderecos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.zeromangas.data.model.Endereco
import com.example.zeromangas.ui.components.PrimaryButton
import com.example.zeromangas.ui.theme.Spacing
import com.example.zeromangas.ui.theme.TextoSecundario
import com.example.zeromangas.ui.theme.VerdeSucesso
import com.example.zeromangas.ui.theme.VermelhoErro
import com.example.zeromangas.viewmodel.BuscaCepState
import com.example.zeromangas.viewmodel.EnderecoViewModel
import com.example.zeromangas.viewmodel.SalvarEnderecoState

/**
 * Formulário de adicionar/editar endereço, em bottom sheet. Quando o CEP tem
 * 8 dígitos, consulta o ViaCEP automaticamente e preenche Estado/Cidade/
 * Bairro/Rua — o usuário só confirma (ou ajusta) e completa número/complemento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioEnderecoBottomSheet(
    enderecoViewModel: EnderecoViewModel,
    enderecoParaEditar: Endereco?,
    onFechar: () -> Unit
) {
    val salvarState by enderecoViewModel.salvarState.collectAsState()
    val buscaCepState by enderecoViewModel.buscaCepState.collectAsState()
    val cepPreenchido by enderecoViewModel.cepPreenchido.collectAsState()

    var nomeDestinatario by remember { mutableStateOf(enderecoParaEditar?.nomeDestinatario ?: "") }
    var telefone by remember { mutableStateOf(enderecoParaEditar?.telefone ?: "") }
    var cep by remember { mutableStateOf(enderecoParaEditar?.cep ?: "") }
    var uf by remember { mutableStateOf(enderecoParaEditar?.uf ?: "") }
    var cidade by remember { mutableStateOf(enderecoParaEditar?.cidade ?: "") }
    var bairro by remember { mutableStateOf(enderecoParaEditar?.bairro ?: "") }
    var logradouro by remember { mutableStateOf(enderecoParaEditar?.logradouro ?: "") }
    var numero by remember { mutableStateOf(enderecoParaEditar?.numero ?: "") }
    var complemento by remember { mutableStateOf(enderecoParaEditar?.complemento ?: "") }
    var informacoesAdicionais by remember { mutableStateOf(enderecoParaEditar?.informacoesAdicionais ?: "") }
    var padrao by remember { mutableStateOf(enderecoParaEditar?.padrao ?: false) }

    val estaEditando = enderecoParaEditar != null

    LaunchedEffect(Unit) {
        enderecoViewModel.resetarSalvarState()
        enderecoViewModel.resetarBuscaCep()
    }

    LaunchedEffect(cepPreenchido) {
        cepPreenchido?.let { endereco ->
            uf = endereco.uf
            cidade = endereco.cidade
            bairro = endereco.bairro
            logradouro = endereco.logradouro
        }
    }

    LaunchedEffect(salvarState) {
        if (salvarState is SalvarEnderecoState.Sucesso) {
            onFechar()
        }
    }

    ModalBottomSheet(onDismissRequest = onFechar) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl)
        ) {
            Text(
                text = if (estaEditando) "Editar endereço" else "Novo endereço",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = nomeDestinatario,
                onValueChange = { nomeDestinatario = it },
                label = { Text("Nome do destinatário") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            OutlinedTextField(
                value = telefone,
                onValueChange = { telefone = it },
                label = { Text("Telefone") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(verticalAlignment = Alignment.Top) {
                OutlinedTextField(
                    value = cep,
                    onValueChange = { novo ->
                        cep = novo.filter { it.isDigit() }.take(8)
                        if (cep.length == 8) enderecoViewModel.buscarCep(cep)
                    },
                    label = { Text("CEP") },
                    singleLine = true,
                    isError = buscaCepState is BuscaCepState.Erro,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        if (buscaCepState is BuscaCepState.Buscando) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            if (buscaCepState is BuscaCepState.Erro) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    (buscaCepState as BuscaCepState.Erro).mensagem,
                    style = MaterialTheme.typography.bodySmall,
                    color = VermelhoErro
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = uf,
                    onValueChange = { uf = it.uppercase().take(2) },
                    label = { Text("UF") },
                    singleLine = true,
                    modifier = Modifier.width(90.dp)
                )
                OutlinedTextField(
                    value = cidade,
                    onValueChange = { cidade = it },
                    label = { Text("Cidade") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))

            OutlinedTextField(
                value = bairro,
                onValueChange = { bairro = it },
                label = { Text("Bairro") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            OutlinedTextField(
                value = logradouro,
                onValueChange = { logradouro = it },
                label = { Text("Rua") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = numero,
                    onValueChange = { numero = it },
                    label = { Text("Número") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = complemento,
                    onValueChange = { complemento = it },
                    label = { Text("Complemento") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))

            OutlinedTextField(
                value = informacoesAdicionais,
                onValueChange = { if (it.length <= 120) informacoesAdicionais = it },
                label = { Text("Informações adicionais (opcional)") },
                supportingText = { Text("Ex: ponto de referência, portão azul...") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(checked = padrao, onCheckedChange = { padrao = it })
                Text("Definir como endereço padrão", style = MaterialTheme.typography.bodyMedium)
            }

            if (salvarState is SalvarEnderecoState.Erro) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    (salvarState as SalvarEnderecoState.Erro).mensagem,
                    style = MaterialTheme.typography.bodySmall,
                    color = VermelhoErro
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            PrimaryButton(
                text = if (estaEditando) "Salvar alterações" else "Adicionar endereço",
                onClick = {
                    if (estaEditando) {
                        enderecoViewModel.editarEndereco(
                            enderecoId = enderecoParaEditar!!.id,
                            nomeDestinatario = nomeDestinatario,
                            telefone = telefone,
                            cep = cep,
                            uf = uf,
                            cidade = cidade,
                            bairro = bairro,
                            logradouro = logradouro,
                            numero = numero,
                            complemento = complemento,
                            informacoesAdicionais = informacoesAdicionais
                        )
                    } else {
                        enderecoViewModel.adicionarEndereco(
                            nomeDestinatario = nomeDestinatario,
                            telefone = telefone,
                            cep = cep,
                            uf = uf,
                            cidade = cidade,
                            bairro = bairro,
                            logradouro = logradouro,
                            numero = numero,
                            complemento = complemento,
                            informacoesAdicionais = informacoesAdicionais,
                            padrao = padrao
                        )
                    }
                },
                enabled = salvarState !is SalvarEnderecoState.Salvando,
                loading = salvarState is SalvarEnderecoState.Salvando,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}