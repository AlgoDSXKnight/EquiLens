package com.equilens.portfolio.portfolio

import org.springframework.data.jpa.repository.JpaRepository

interface PortfolioTransactionRepository :
    JpaRepository<PortfolioTransaction, Long> {

    fun findByPortfolioIdOrderByIdAsc(
        portfolioId: Long
    ): List<PortfolioTransaction>
}