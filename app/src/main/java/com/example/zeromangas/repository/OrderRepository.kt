package com.example.zeromangas.repository

import com.example.zeromangas.data.model.Order
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class OrderRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val ordersCollection = db.collection("orders")

    /**
     * Salva o pedido na coleção "orders" (cada documento contém o userId,
     * permitindo consultar via where("userId", isEqualTo = ...)).
     * Retorna o id do pedido criado em caso de sucesso.
     */
    suspend fun salvarPedido(order: Order): Result<String> {
        return try {
            val documentoRef = ordersCollection.document()
            val pedidoComId = order.copy(id = documentoRef.id)
            documentoRef.set(pedidoComId).await()
            Result.success(documentoRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listarPedidosDoUsuario(userId: String): Result<List<Order>> {
        return try {
            val snapshot = ordersCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val pedidos = snapshot.documents.mapNotNull { it.toObject(Order::class.java) }
            Result.success(pedidos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
