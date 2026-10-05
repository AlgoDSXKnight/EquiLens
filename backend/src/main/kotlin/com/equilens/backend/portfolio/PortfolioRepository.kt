package com.equilens.backend.portfolio

import org.springframework.data.jpa.repository.JpaRepository

interface PortfolioRepository : JpaRepository<Portfolio, Long> {

    fun findByUserId(userId: Long): Portfolio?
}