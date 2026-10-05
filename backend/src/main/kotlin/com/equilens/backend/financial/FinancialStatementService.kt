package com.equilens.backend.financial

import com.equilens.backend.company.CompanyRepository
import org.springframework.stereotype.Service

@Service
class FinancialStatementService(
    private val financialStatementRepository: FinancialStatementRepository,
    private val companyRepository: CompanyRepository
) {

    fun getFinancials(companyId: Long): List<FinancialStatement> {
        if (!companyRepository.existsById(companyId)) {
            throw IllegalArgumentException("Company not found")
        }

        return financialStatementRepository
            .findByCompanyIdOrderByFiscalYearAsc(companyId)
    }

    fun getFinancial(
        companyId: Long,
        fiscalYear: Int
    ): FinancialStatement {

        if (!companyRepository.existsById(companyId)) {
            throw IllegalArgumentException("Company not found")
        }

        return financialStatementRepository
            .findByCompanyIdAndFiscalYear(companyId, fiscalYear)
            ?: throw IllegalArgumentException(
                "Financial data not found for fiscal year $fiscalYear"
            )
    }

    fun createFinancial(
        companyId: Long,
        financialStatement: FinancialStatement
    ): FinancialStatement {

        val company = companyRepository.findById(companyId)
            .orElseThrow {
                IllegalArgumentException("Company not found")
            }

        if (
            financialStatementRepository
                .findByCompanyIdAndFiscalYear(
                    companyId,
                    financialStatement.fiscalYear
                ) != null
        ) {
            throw IllegalArgumentException(
                "Financial data already exists for fiscal year ${financialStatement.fiscalYear}"
            )
        }

        financialStatement.company = company

        return financialStatementRepository.save(
            financialStatement
        )
    }

    fun updateFinancial(
        companyId: Long,
        fiscalYear: Int,
        updated: FinancialStatement
    ): FinancialStatement {

        val existing = getFinancial(
            companyId,
            fiscalYear
        )

        existing.revenue = updated.revenue
        existing.operatingProfit = updated.operatingProfit
        existing.netProfit = updated.netProfit
        existing.eps = updated.eps
        existing.freeCashFlow = updated.freeCashFlow
        existing.totalAssets = updated.totalAssets
        existing.totalDebt = updated.totalDebt
        existing.equity = updated.equity

        return financialStatementRepository.save(existing)
    }

    fun deleteFinancial(
        companyId: Long,
        fiscalYear: Int
    ) {
        val existing = getFinancial(
            companyId,
            fiscalYear
        )

        financialStatementRepository.delete(existing)
    }
}