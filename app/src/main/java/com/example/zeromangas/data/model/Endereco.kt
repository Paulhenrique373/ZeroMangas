package com.example.zeromangas.data.model

data class Endereco(
    val id: String = "",
    val clienteId: String = "",
    val nomeDestinatario: String = "",
    val telefone: String = "",
    val cep: String = "",
    val uf: String = "",
    val cidade: String = "",
    val bairro: String = "",
    val logradouro: String = "",
    val numero: String = "",
    val complemento: String = "",
    val informacoesAdicionais: String = "",
    val padrao: Boolean = false
)