package com.equilens.backend.financial

import org.springframework.data.jpa.repository.JpaRepository

interface FinancialStatementRepository :
    JpaRepository<FinancialStatement, Long> {

    fun findByCompanyIdOrderByFiscalYearAsc(
        companyId: Long
    ): List<FinancialStatement>

    fun findByCompanyIdAndFiscalYear(
        companyId: Long,
        fiscalYear: Int
    ): FinancialStatement?
}