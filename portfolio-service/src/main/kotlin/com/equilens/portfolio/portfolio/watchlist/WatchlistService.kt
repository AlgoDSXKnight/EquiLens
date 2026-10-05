package com.equilens.portfolio.watchlist

import com.equilens.portfolio.research.ResearchClient
import org.springframework.stereotype.Service
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

@Service
class WatchlistService(
    private val watchlistRepository: WatchlistRepository,
    private val researchClient: ResearchClient
) {

    fun getWatchlist(userId: Long): List<WatchlistItem> {
        return watchlistRepository.findByUserIdOrderByIdDesc(userId)
    }

    fun addToWatchlist(
        userId: Long,
        companyId: Long
    ): WatchlistItem {

        // Verify company exists in Research Service.
        researchClient.getCompany(companyId)

        val existing = watchlistRepository
            .findByUserIdAndCompanyId(userId, companyId)

        if (existing != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Company is already in the watchlist")
        }

        return watchlistRepository.save(
            WatchlistItem(
                userId = userId,
                companyId = companyId
            )
        )
    }

    fun removeFromWatchlist(
        userId: Long,
        companyId: Long
    ) {

        val existing = watchlistRepository
            .findByUserIdAndCompanyId(userId, companyId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Company is not in the watchlist")

        watchlistRepository.delete(existing)
    }
}
