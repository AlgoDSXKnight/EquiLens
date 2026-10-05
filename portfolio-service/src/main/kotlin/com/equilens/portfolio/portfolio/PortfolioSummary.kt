package com.equilens.portfolio.portfolio

data class PortfolioPosition(
    val companyId: Long,
    val symbol: String,
    val name: String,
    val quantity: Double,
    val averageCost: Double,
    val currentPrice: Double,
    val investedValue: Double,
    val marketValue: Double,
    val unrealizedPnl: Double,
    val pnlPercent: Double
)

data class PortfolioSummary(
    val portfolioId: Long,
    val userId: Long,
    val name: String,
    val investedValue: Double,
    val marketValue: Double,
    val unrealizedPnl: Double,
    val pnlPercent: Double,
    val positions: List<PortfolioPosition>
)