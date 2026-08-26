package com.example.zeromangas.data.model

/**
 * Dados do perfil que vivem no Supabase (tabelas "usuarios" + "clientes"),
 * carregados a partir do firebase_uid do usuário logado. Complementa o
 * [User], que tem só o que o Firebase Auth guarda (uid, nome, email, foto).
 */
data class PerfilCliente(
    val usuarioId: String = "",
    val clienteId: String = "",
    val nome: String = "",
    val email: String = "",
    val fotoUrl: String = "",
    val telefone: String = "",
    val cpf: String = "",
    val bio: String = "",
    val genero: String = "",
    val dataNascimento: String = ""
)