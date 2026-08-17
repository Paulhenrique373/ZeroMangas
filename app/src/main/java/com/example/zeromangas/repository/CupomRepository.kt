package com.example.zeromangas.repository

import com.example.zeromangas.data.model.Cupom
import com.example.zeromangas.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class CupomDto(
    val id: String? = null,
    val codigo: String,
    @SerialName("tipo_desconto") val tipoDesconto: String = "PERCENTUAL",
    val valor: Double = 0.0,
    val ativo: Boolean = true,
    @SerialName("valor_minimo") val valorMinimo: Double = 0.0
) {
    fun paraCupom() = Cupom(
        codigo = codigo,
        tipoDesconto = tipoDesconto,
        valor = valor,
        ativo = ativo,
        valorMinimo = valorMinimo
    )
}

class CupomRepository {

    private val cuponsTable = SupabaseClient.client.postgrest.from("cupons")

    /**
     * Busca um cupom pelo código (sem diferenciar maiúsculas/minúsculas) na tabela "cupons".
     * Retorna erro se o cupom não existir ou estiver inativo.
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
            Result.failure(Exception("Não foi possível validar o cupom. Tente novamente."))
        }
    }
}