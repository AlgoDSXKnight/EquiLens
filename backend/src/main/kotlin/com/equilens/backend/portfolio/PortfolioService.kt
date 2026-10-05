package com.equilens.backend.portfolio

import com.equilens.backend.auth.UserRepository
import com.equilens.backend.company.CompanyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.abs

@Service
class PortfolioService(
    private val portfolioRepository: PortfolioRepository,
    private val transactionRepository: PortfolioTransactionRepository,
    private val userRepository: UserRepository,
    private val companyRepository: CompanyRepository
) {

    @Transactional
    fun getOrCreatePortfolio(userId: Long): Portfolio {

        val existing = portfolioRepository.findByUserId(userId)

        if (existing != null) {
            return existing
        }

        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
        }

        return portfolioRepository.save(
            Portfolio(
                user = user,
                name = "Main Portfolio"
            )
        )
    }

    fun getTransactions(userId: Long): List<PortfolioTransaction> {

        val portfolio = getOrCreatePortfolio(userId)

        return transactionRepository
            .findByPortfolioIdOrderByTransactionDateAscIdAsc(
                portfolio.id!!
            )
    }

    @Transactional
    fun addTransaction(
        userId: Long,
        companyId: Long,
        type: TransactionType,
        quantity: Double,
        price: Double
    ): PortfolioTransaction {

        if (quantity <= 0) {
            throw IllegalArgumentException(
                "Quantity must be greater than zero"
            )
        }

        if (price <= 0) {
            throw IllegalArgumentException(
                "Price must be greater than zero"
            )
        }

        val portfolio = getOrCreatePortfolio(userId)

        val company = companyRepository.findById(companyId)
            .orElseThrow {
                IllegalArgumentException("Company not found")
            }

        if (type == TransactionType.SELL) {

            val holdings = calculateHolding(
                portfolio.id!!,
                companyId
            )

            if (quantity > holdings.quantity + 1e-9) {
                throw IllegalArgumentException(
                    "Cannot sell $quantity shares. " +
                    "Current holding is ${holdings.quantity}"
                )
            }
        }

        return transactionRepository.save(
            PortfolioTransaction(
                portfolio = portfolio,
                company = company,
                type = type,
                quantity = quantity,
                price = price
            )
        )
    }

    fun getSummary(userId: Long): PortfolioSummaryResponse {

        val portfolio = getOrCreatePortfolio(userId)

        val transactions =
            transactionRepository
                .findByPortfolioIdOrderByTransactionDateAscIdAsc(
                    portfolio.id!!
                )

        val companyIds = transactions
            .mapNotNull { it.company?.id }
            .distinct()

        val holdings = companyIds
            .map { companyId ->
                calculateHolding(
                    portfolio.id!!,
                    companyId
                )
            }
            .filter { it.quantity > 0 }

        val invested = holdings.sumOf { it.invested }
        val marketValue = holdings.sumOf { it.marketValue }
        val profitLoss = marketValue - invested

        val profitLossPercent =
            if (invested > 0) {
                (profitLoss / invested) * 100.0
            } else {
                0.0
            }

        return PortfolioSummaryResponse(
            invested = invested,
            marketValue = marketValue,
            profitLoss = profitLoss,
            profitLossPercent = profitLossPercent,
            holdings = holdings
        )
    }

    private fun calculateHolding(
        portfolioId: Long,
        companyId: Long
    ): HoldingResponse {

        val transactions =
            transactionRepository
                .findByPortfolioIdAndCompanyIdOrderByTransactionDateAscIdAsc(
                    portfolioId,
                    companyId
                )

        val company = companyRepository.findById(companyId)
            .orElseThrow {
                IllegalArgumentException("Company not found")
            }

        var quantity = 0.0
        var invested = 0.0

        for (transaction in transactions) {

            when (transaction.type) {

                TransactionType.BUY -> {
                    quantity += transaction.quantity
                    invested +=
                        transaction.quantity * transaction.price
                }

                TransactionType.SELL -> {

                    if (quantity <= 0) {
                        throw IllegalStateException(
                            "Invalid transaction history"
                        )
                    }

                    val averageCost =
                        invested / quantity

                    val costBasisSold =
                        transaction.quantity * averageCost

                    quantity -= transaction.quantity
                    invested -= costBasisSold
                }
            }
        }

        if (abs(quantity) < 1e-9) {
            quantity = 0.0
            invested = 0.0
        }

        val averageCost =
            if (quantity > 0) {
                invested / quantity
            } else {
                0.0
            }

        val currentPrice = company.currentPrice

        val marketValue =
            quantity * currentPrice

        val profitLoss =
            marketValue - invested

        val profitLossPercent =
            if (invested > 0) {
                (profitLoss / invested) * 100.0
            } else {
                0.0
            }

        return HoldingResponse(
            companyId = company.id!!,
            symbol = company.symbol,
            companyName = company.name,
            quantity = quantity,
            averageCost = averageCost,
            invested = invested,
            currentPrice = currentPrice,
            marketValue = marketValue,
            profitLoss = profitLoss,
            profitLossPercent = profitLossPercent
        )
    }
}

data class HoldingResponse(
    val companyId: Long,
    val symbol: String,
    val companyName: String,
    val quantity: Double,
    val averageCost: Double,
    val invested: Double,
    val currentPrice: Double,
    val marketValue: Double,
    val profitLoss: Double,
    val profitLossPercent: Double
)

data class PortfolioSummaryResponse(
    val invested: Double,
    val marketValue: Double,
    val profitLoss: Double,
    val profitLossPercent: Double,
    val holdings: List<HoldingResponse>
)