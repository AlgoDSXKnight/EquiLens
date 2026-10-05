package com.equilens.backend.company

import org.springframework.data.jpa.repository.JpaRepository

interface CompanyRepository : JpaRepository<Company, Long> {
    fun findBySymbolIgnoreCase(symbol: String): Company?
}
