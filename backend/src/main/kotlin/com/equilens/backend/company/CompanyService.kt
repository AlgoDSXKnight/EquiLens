package com.equilens.backend.company

import org.springframework.stereotype.Service

@Service
class CompanyService(private val repository: CompanyRepository) {
    fun findAll(): List<Company> = repository.findAll()

    fun findById(id: Long): Company = repository.findById(id)
        .orElseThrow { NoSuchElementException("Company $id not found") }

    fun findBySymbol(symbol: String): Company = repository.findBySymbolIgnoreCase(symbol)
        ?: throw NoSuchElementException("Company $symbol not found")

    fun create(company: Company): Company {
        require(company.symbol.isNotBlank()) { "Symbol is required" }
        require(company.name.isNotBlank()) { "Company name is required" }
        require(repository.findBySymbolIgnoreCase(company.symbol) == null) { "Symbol already exists" }
        company.symbol = company.symbol.trim().uppercase()
        return repository.save(company)
    }

    fun update(id: Long, incoming: Company): Company {
        val existing = findById(id)
        existing.symbol = incoming.symbol.trim().uppercase()
        existing.name = incoming.name.trim()
        existing.sector = incoming.sector.trim()
        existing.currentPrice = incoming.currentPrice
        existing.marketCap = incoming.marketCap
        existing.revenueGrowth = incoming.revenueGrowth
        existing.profitGrowth = incoming.profitGrowth
        existing.roe = incoming.roe
        existing.debtToEquity = incoming.debtToEquity
        existing.peRatio = incoming.peRatio
        return repository.save(existing)
    }

    fun delete(id: Long) = repository.delete(findById(id))
}
