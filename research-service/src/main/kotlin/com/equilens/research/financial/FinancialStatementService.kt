package com.equilens.research.financial

import com.equilens.research.company.CompanyRepository
import org.springframework.stereotype.Service
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

@Service
class FinancialStatementService(
    private val financialStatementRepository: FinancialStatementRepository,
    private val companyRepository: CompanyRepository
) {

    fun getFinancials(companyId: Long): List<FinancialStatement> {
        if (!companyRepository.existsById(companyId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found")
        }

        return financialStatementRepository
            .findByCompanyIdOrderByFiscalYearAsc(companyId)
    }

    fun getFinancial(
        companyId: Long,
        fiscalYear: Int
    ): FinancialStatement {

        if (!companyRepository.existsById(companyId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found")
        }

        return financialStatementRepository
            .findByCompanyIdAndFiscalYear(
                companyId,
                fiscalYear
            )
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Financial data not found for fiscal year $fiscalYear")
    }

    fun createFinancial(
        companyId: Long,
        financialStatement: FinancialStatement
    ): FinancialStatement {

        validate(financialStatement)

        val company = companyRepository.findById(companyId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found")
            }

        if (
            financialStatementRepository
                .findByCompanyIdAndFiscalYear(
                    companyId,
                    financialStatement.fiscalYear
                ) != null
        ) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Financial data already exists for fiscal year ${financialStatement.fiscalYear}")
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

        validate(updated)

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

    private fun validate(statement: FinancialStatement) {
        require(statement.fiscalYear in 1900..2100) { "Fiscal year must be between 1900 and 2100" }
        require(listOf(statement.revenue, statement.operatingProfit, statement.netProfit, statement.eps, statement.freeCashFlow, statement.totalAssets, statement.totalDebt, statement.equity).all { it.isFinite() }) { "Financial values must be finite numbers" }
        require(statement.totalAssets >= 0 && statement.totalDebt >= 0) { "Assets and debt cannot be negative" }
    }
}
