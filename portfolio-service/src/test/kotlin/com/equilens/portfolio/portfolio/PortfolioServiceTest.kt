package com.equilens.portfolio.portfolio

import com.equilens.portfolio.research.ResearchClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class PortfolioServiceTest {
    private val portfolios = Mockito.mock(PortfolioRepository::class.java)
    private val transactions = Mockito.mock(PortfolioTransactionRepository::class.java)
    private val research = Mockito.mock(ResearchClient::class.java)
    private val service = PortfolioService(portfolios, transactions, research)

    @Test
    fun `rejects non-finite quantities before service calls`() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { quantity ->
            assertThrows(IllegalArgumentException::class.java) {
                service.addTransaction(7, 3, "BUY", quantity, 100.0)
            }
        }

        Mockito.verifyNoInteractions(portfolios, transactions, research)
    }

    @Test
    fun `rejects non-finite prices before service calls`() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { price ->
            assertThrows(IllegalArgumentException::class.java) {
                service.addTransaction(7, 3, "BUY", 1.0, price)
            }
        }

        Mockito.verifyNoInteractions(portfolios, transactions, research)
    }

    @Test
    fun `rejects overselling against the user's company holdings`() {
        val portfolio = Portfolio(id = 11, userId = 7)
        Mockito.`when`(research.getCompany(3)).thenReturn(com.equilens.portfolio.research.ResearchCompany(id = 3))
        Mockito.`when`(portfolios.findByUserId(7)).thenReturn(portfolio)
        Mockito.`when`(transactions.findByPortfolioIdOrderByIdAsc(11)).thenReturn(
            listOf(PortfolioTransaction(portfolioId = 11, companyId = 3, type = "BUY", quantity = 2.0, price = 100.0))
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            service.addTransaction(7, 3, "SELL", 3.0, 110.0)
        }

        assertEquals("Cannot sell 3.0 shares. Current holdings: 2.0", error.message)
        Mockito.verify(transactions, Mockito.never()).save(Mockito.any(PortfolioTransaction::class.java))
    }
}
