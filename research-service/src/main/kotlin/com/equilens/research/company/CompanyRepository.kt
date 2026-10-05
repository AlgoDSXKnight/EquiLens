package com.equilens.research.company

import org.springframework.data.jpa.repository.JpaRepository

interface CompanyRepository : JpaRepository<Company, Long> {

    fun findBySymbolIgnoreCase(symbol: String): Company?
}