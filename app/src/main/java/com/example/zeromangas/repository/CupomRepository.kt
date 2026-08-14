package com.example.zeromangas.repository

import com.example.zeromangas.data.model.Cupom
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CupomRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val cuponsCollection = db.collection("cupons")

    /**
     * Busca um cupom pelo código (case-insensitive) na coleção "cupons".
     * Retorna erro se o cupom não existir ou estiver inativo.
     */
    suspend fun buscarCupom(codigo: String): Result<Cupom> {
        return try {
            val codigoNormalizado = codigo.trim().uppercase()

            val snapshot = cuponsCollection
                .whereEqualTo("codigo", codigoNormalizado)
                .get()
                .await()

            val cupom = snapshot.documents.firstOrNull()?.toObject(Cupom::class.java)

            when {
                cupom == null -> Result.failure(Exception("Cupom inválido."))
                !cupom.ativo -> Result.failure(Exception("Este cupom não está mais ativo."))
                else -> Result.success(cupom)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Não foi possível validar o cupom. Tente novamente."))
        }
    }
}