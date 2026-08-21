package com.example.zeromangas.repository

import com.example.zeromangas.data.model.Cupom
import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@Serializable
private data class CupomDto(
    val id: String? = null,
    val codigo: String,
    @SerialName("tipo_desconto") val tipoDesconto: String = "PERCENTUAL",
    val valor: Double = 0.0,
    val ativo: Boolean = true,
    @SerialName("valor_minimo") val valorMinimo: Double = 0.0,
    @SerialName("limite_total") val limiteTotal: Int = 0
) {
    fun paraCupom() = Cupom(
        codigo = codigo,
        tipoDesconto = tipoDesconto,
        valor = valor,
        ativo = ativo,
        valorMinimo = valorMinimo,
        limiteTotal = limiteTotal
    )
}

@Serializable
private data class ContarUsosTotalParamsDto(
    @SerialName("p_codigo") val codigo: String
)

@Serializable
private data class ContarUsosUsuarioParamsDto(
    @SerialName("p_codigo") val codigo: String,
    @SerialName("p_user_id") val userId: String
)

private val jsonRpc = Json { encodeDefaults = true }

class CupomRepository {

    private val cuponsTable = SupabaseClient.client.postgrest.from("cupons")

    /**
     * Busca um cupom pelo código (sem diferenciar maiúsculas/minúsculas) na tabela "cupons".
     * A tabela "cupons" continua de leitura pública (não é dado sensível de usuário),
     * então esse select direto continua funcionando normalmente.
     */
    suspend fun buscarCupom(codigo: String): Result<Cupom> {
        return try {
            val codigoNormalizado = codigo.trim()

            val lista = cuponsTable
                .select {
                    filter {
                        ilike("codigo", codigoNormalizado)
                    }
                }
                .decodeList<CupomDto>()

            val cupomDto = lista.firstOrNull()

            when {
                cupomDto == null -> Result.failure(Exception("Cupom inválido."))
                !cupomDto.ativo -> Result.failure(Exception("Este cupom não está mais ativo."))
                else -> Result.success(cupomDto.paraCupom())
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível validar o cupom. Tente novamente.", e))
        }
    }

    /**
     * Conta quantas vezes um cupom já foi utilizado no total, via a função
     * "contar_usos_cupom_total" — precisa ser função porque "pedidos" agora
     * tem RLS sem policy (não dá mais pra ler a tabela inteira direto).
     * Pedidos CANCELADOS não contam como uso.
     */
    suspend fun contarUsosTotais(codigo: String): Result<Int> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                ContarUsosTotalParamsDto.serializer(),
                ContarUsosTotalParamsDto(codigo = codigo)
            ).jsonObject

            val resultado = SupabaseClient.client.postgrest.rpc("contar_usos_cupom_total", paramsJson)
            Result.success(resultado.data.trim().toIntOrNull() ?: 0)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível verificar o uso total do cupom.", e))
        }
    }

    /**
     * Conta quantas vezes um usuário específico já utilizou este cupom, via a função
     * "contar_usos_cupom_usuario". Mesmo motivo do método acima.
     */
    suspend fun contarUsosPorUsuario(codigo: String, userId: String): Result<Int> {
        return try {
            val paramsJson = jsonRpc.encodeToJsonElement(
                ContarUsosUsuarioParamsDto.serializer(),
                ContarUsosUsuarioParamsDto(codigo = codigo, userId = userId)
            ).jsonObject

            val resultado = SupabaseClient.client.postgrest.rpc("contar_usos_cupom_usuario", paramsJson)
            Result.success(resultado.data.trim().toIntOrNull() ?: 0)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Não foi possível verificar o uso do cupom para este usuário.", e))
        }
    }
}