package com.equilens.portfolio.portfolio

import com.equilens.portfolio.research.ResearchClient
import org.springframework.stereotype.Service

@Service
class PortfolioService(
    private val portfolioRepository: PortfolioRepository,
    private val transactionRepository: PortfolioTransactionRepository,
    private val researchClient: ResearchClient
) {

    fun getOrCreatePortfolio(userId: Long): Portfolio {
        return portfolioRepository.findByUserId(userId)
            ?: portfolioRepository.save(
                Portfolio(
                    userId = userId,
                    name = "Main Portfolio"
                )
            )
    }

    fun getTransactions(userId: Long): List<PortfolioTransaction> {
        val portfolio = getOrCreatePortfolio(userId)

        return transactionRepository
            .findByPortfolioIdOrderByIdAsc(portfolio.id!!)
    }

    fun getSummary(userId: Long): PortfolioSummary {
    val portfolio = getOrCreatePortfolio(userId)

    val transactions = transactionRepository
        .findByPortfolioIdOrderByIdAsc(portfolio.id!!)

    val companyIds = transactions
        .map { it.companyId }
        .distinct()

    val positions = mutableListOf<PortfolioPosition>()

    for (companyId in companyIds) {

        val companyTransactions = transactions
            .filter { it.companyId == companyId }

        val costBasis = PortfolioMath.costBasis(companyTransactions)
        val quantity = costBasis.quantity
        val totalCost = costBasis.costBasis

        if (!quantity.isFinite() || quantity <= 0) {
            continue
        }

        val company = researchClient.getCompany(companyId)

        val averageCost = totalCost / quantity
        val investedValue = totalCost
        val marketValue = quantity * company.currentPrice
        val unrealizedPnl = marketValue - investedValue

        val pnlPercent = if (investedValue > 0) {
            (unrealizedPnl / investedValue) * 100
        } else {
            0.0
        }

        positions.add(
            PortfolioPosition(
                companyId = companyId,
                symbol = company.symbol,
                name = company.name,
                quantity = quantity,
                averageCost = averageCost,
                currentPrice = company.currentPrice,
                investedValue = investedValue,
                marketValue = marketValue,
                unrealizedPnl = unrealizedPnl,
                pnlPercent = pnlPercent
            )
        )
    }

    val investedValue = positions.sumOf { it.investedValue }
    val marketValue = positions.sumOf { it.marketValue }
    val unrealizedPnl = marketValue - investedValue

    val pnlPercent = if (investedValue > 0) {
        (unrealizedPnl / investedValue) * 100
    } else {
        0.0
    }

    return PortfolioSummary(
        portfolioId = portfolio.id!!,
        userId = userId,
        name = portfolio.name,
        investedValue = investedValue,
        marketValue = marketValue,
        unrealizedPnl = unrealizedPnl,
        pnlPercent = pnlPercent,
        positions = positions
    )
}

    fun addTransaction(
        userId: Long,
        companyId: Long,
        type: String,
        quantity: Double,
        price: Double
    ): PortfolioTransaction {

        if (!quantity.isFinite() || quantity <= 0) {
            throw IllegalArgumentException(
                "Quantity must be a finite number greater than zero"
            )
        }

        if (!price.isFinite() || price <= 0) {
            throw IllegalArgumentException(
                "Price must be greater than zero"
            )
        }

        val normalizedType = type.trim().uppercase()

        if (normalizedType != "BUY" && normalizedType != "SELL") {
            throw IllegalArgumentException(
                "Transaction type must be BUY or SELL"
            )
        }

        // Verify that the company exists in Research Service.
        researchClient.getCompany(companyId)

        val portfolio = getOrCreatePortfolio(userId)

        if (normalizedType == "SELL") {
            validateSell(
                portfolio.id!!,
                companyId,
                quantity
            )
        }

        val transaction = PortfolioTransaction(
            portfolioId = portfolio.id!!,
            companyId = companyId,
            type = normalizedType,
            quantity = quantity,
            price = price
        )

        return transactionRepository.save(transaction)
    }

    private fun validateSell(
        portfolioId: Long,
        companyId: Long,
        sellQuantity: Double
    ) {
        val transactions = transactionRepository
            .findByPortfolioIdOrderByIdAsc(portfolioId)
            .filter { it.companyId == companyId }

        val holdings = transactions.sumOf { if (it.type == "BUY") it.quantity else -it.quantity }

        if (sellQuantity > holdings) {
            throw IllegalArgumentException(
                "Cannot sell $sellQuantity shares. Current holdings: $holdings"
            )
        }
    }
}
