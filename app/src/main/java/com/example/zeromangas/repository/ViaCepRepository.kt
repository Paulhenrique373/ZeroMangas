package com.example.zeromangas.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Endereço retornado pela API pública ViaCEP (https://viacep.com.br).
 */
data class EnderecoCep(
    val cep: String,
    val logradouro: String,
    val bairro: String,
    val cidade: String,
    val uf: String
)

class ViaCepRepository {

    /**
     * Consulta o CEP na API do ViaCEP.
     * Retorna falha se o CEP não existir ou se não for possível conectar.
     */
    suspend fun buscarEndereco(cep: String): Result<EnderecoCep> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://viacep.com.br/ws/$cep/json/")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "GET"
                conexao.connectTimeout = 8000
                conexao.readTimeout = 8000

                val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                conexao.disconnect()

                val json = JSONObject(resposta)

                if (json.optBoolean("erro", false)) {
                    return@withContext Result.failure(Exception("CEP não encontrado. Confira o número digitado."))
                }

                val endereco = EnderecoCep(
                    cep = json.optString("cep"),
                    logradouro = json.optString("logradouro"),
                    bairro = json.optString("bairro"),
                    cidade = json.optString("localidade"),
                    uf = json.optString("uf")
                )
                Result.success(endereco)
            } catch (e: Exception) {
                Result.failure(Exception("Não foi possível verificar o CEP. Verifique sua conexão e tente novamente."))
            }
        }
    }
}