package com.equilens.backend.watchlist

import org.springframework.data.jpa.repository.JpaRepository

interface WatchlistRepository :
    JpaRepository<WatchlistItem, Long> {

    fun findByUserIdOrderByIdDesc(
        userId: Long
    ): List<WatchlistItem>

    fun findByUserIdAndCompanyId(
        userId: Long,
        companyId: Long
    ): WatchlistItem?

    fun existsByUserIdAndCompanyId(
        userId: Long,
        companyId: Long
    ): Boolean

    fun deleteByUserIdAndCompanyId(
        userId: Long,
        companyId: Long
    )
}