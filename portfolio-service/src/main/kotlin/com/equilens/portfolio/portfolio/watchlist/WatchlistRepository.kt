package com.equilens.portfolio.watchlist

import org.springframework.data.jpa.repository.JpaRepository

interface WatchlistRepository : JpaRepository<WatchlistItem, Long> {

    fun findByUserIdOrderByIdDesc(userId: Long): List<WatchlistItem>

    fun findByUserIdAndCompanyId(
        userId: Long,
        companyId: Long
    ): WatchlistItem?
}