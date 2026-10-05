package com.equilens.research.company

import org.springframework.stereotype.Service
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

@Service
class CompanyService(
    private val companyRepository: CompanyRepository
) {

    fun getAllCompanies(): List<Company> {
        return companyRepository.findAll()
    }

    fun getCompany(id: Long): Company {
        return companyRepository.findById(id)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found")
            }
    }

    fun createCompany(company: Company): Company {
        validate(company)
        company.symbol = company.symbol.trim().uppercase()
        if (companyRepository.findBySymbolIgnoreCase(company.symbol) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Company with symbol ${company.symbol} already exists")
        }

        return companyRepository.save(company)
    }

    fun updateCompany(
        id: Long,
        updated: Company
    ): Company {

        val existing = getCompany(id)

        validate(updated)
        val normalizedSymbol = updated.symbol.trim().uppercase()
        if (normalizedSymbol != existing.symbol && companyRepository.findBySymbolIgnoreCase(normalizedSymbol) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Company with symbol $normalizedSymbol already exists")
        }

        existing.symbol = normalizedSymbol
        existing.name = updated.name
        existing.sector = updated.sector
        existing.currentPrice = updated.currentPrice
        existing.marketCap = updated.marketCap
        existing.revenueGrowth = updated.revenueGrowth
        existing.profitGrowth = updated.profitGrowth
        existing.roe = updated.roe
        existing.debtToEquity = updated.debtToEquity
        existing.peRatio = updated.peRatio
        existing.dataClassification = updated.dataClassification

        return companyRepository.save(existing)
    }

    private fun validate(company: Company) {
        require(company.symbol.isNotBlank()) { "Company symbol is required" }
        require(company.name.isNotBlank()) { "Company name is required" }
        require(company.sector.isNotBlank()) { "Company sector is required" }
        require(listOf(company.currentPrice, company.marketCap, company.revenueGrowth, company.profitGrowth, company.roe, company.debtToEquity, company.peRatio).all { it.isFinite() }) { "Company metrics must be finite numbers" }
        require(company.currentPrice >= 0 && company.marketCap >= 0) { "Price and market capitalization cannot be negative" }
        require(company.dataClassification == null || company.dataClassification in setOf("UNVERIFIED", "USER_PROVIDED", "SYNTHETIC_DEMO")) {
            "Data classification must be UNVERIFIED, USER_PROVIDED, or SYNTHETIC_DEMO"
        }
    }

    fun deleteCompany(id: Long) {
        val company = getCompany(id)
        companyRepository.delete(company)
    }
}
