package com.example.zeromangas.data.model

/**
 * Representa um cupom de desconto armazenado na coleção "cupons" do Firestore.
 *
 * tipoDesconto: "PERCENTUAL" (desconta % do subtotal) ou "FIXO" (desconta um valor fixo em R$)
 * valorMinimo: valor mínimo de subtotal necessário para o cupom ser válido (0.0 = sem mínimo)
 */
data class Cupom(
    val codigo: String = "",
    val tipoDesconto: String = "PERCENTUAL",
    val valor: Double = 0.0,
    val ativo: Boolean = true,
    val valorMinimo: Double = 0.0
) {
    fun calcularDesconto(subtotal: Double): Double {
        val desconto = when (tipoDesconto.uppercase()) {
            "FIXO" -> valor
            else -> subtotal * (valor / 100.0)
        }
        // O desconto nunca pode ultrapassar o subtotal
        return desconto.coerceIn(0.0, subtotal)
    }
}