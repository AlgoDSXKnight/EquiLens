package com.equilens.portfolio.portfolio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertThrows
import java.time.LocalDateTime

class PortfolioMathTest {
    @Test
    fun `average cost is reduced proportionally after sells`() {
        val transactions = listOf(
            transaction("BUY", 20.0, 1900.0),
            transaction("SELL", 5.0, 2100.0),
            transaction("BUY", 5.0, 2000.0),
            transaction("BUY", 10.0, 2000.0),
            transaction("SELL", 5.0, 2100.0)
        )

        val position = PortfolioMath.costBasis(transactions)
        val marketValue = position.quantity * 2000.0

        assertEquals(25.0, position.quantity, 0.0000001)
        assertEquals(1950.0, position.averageCost, 0.0000001)
        assertEquals(48750.0, position.costBasis, 0.0000001)
        assertEquals(50000.0, marketValue, 0.0000001)
        assertEquals(1250.0, marketValue - position.costBasis, 0.0000001)
    }

    @Test
    fun `portfolio math rejects oversells`() {
        assertThrows(IllegalArgumentException::class.java) {
            PortfolioMath.costBasis(listOf(transaction("BUY", 2.0, 100.0), transaction("SELL", 3.0, 100.0)))
        }
    }

    private fun transaction(type: String, quantity: Double, price: Double) =
        PortfolioTransaction(type = type, quantity = quantity, price = price, transactionDate = LocalDateTime.now())
}
