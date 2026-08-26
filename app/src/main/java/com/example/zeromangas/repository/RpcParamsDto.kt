package com.example.zeromangas.repository

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UsuarioParamsDto(
    @SerialName("p_usuario_id") val usuarioId: String
)