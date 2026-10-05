package com.equilens.portfolio.portfolio

data class PositionCostBasis(val quantity: Double, val costBasis: Double) {
    val averageCost: Double get() = if (quantity > 0) costBasis / quantity else 0.0
}

object PortfolioMath {
    fun costBasis(transactions: List<PortfolioTransaction>): PositionCostBasis {
        var quantity = 0.0
        var costBasis = 0.0
        transactions.forEach { transaction ->
            when (transaction.type.uppercase()) {
                "BUY" -> {
                    quantity += transaction.quantity
                    costBasis += transaction.quantity * transaction.price
                }
                "SELL" -> {
                    require(transaction.quantity <= quantity) { "Sell quantity exceeds holdings" }
                    val averageCost = if (quantity > 0) costBasis / quantity else 0.0
                    costBasis -= transaction.quantity * averageCost
                    quantity -= transaction.quantity
                }
                else -> throw IllegalArgumentException("Transaction type must be BUY or SELL")
            }
        }
        return PositionCostBasis(quantity, costBasis)
    }
}
