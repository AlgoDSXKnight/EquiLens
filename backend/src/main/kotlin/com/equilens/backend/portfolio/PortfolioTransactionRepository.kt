package com.equilens.backend.portfolio

import org.springframework.data.jpa.repository.JpaRepository

interface PortfolioTransactionRepository :
    JpaRepository<PortfolioTransaction, Long> {

    fun findByPortfolioIdOrderByTransactionDateAscIdAsc(
        portfolioId: Long
    ): List<PortfolioTransaction>

    fun findByPortfolioIdAndCompanyIdOrderByTransactionDateAscIdAsc(
        portfolioId: Long,
        companyId: Long
    ): List<PortfolioTransaction>
}