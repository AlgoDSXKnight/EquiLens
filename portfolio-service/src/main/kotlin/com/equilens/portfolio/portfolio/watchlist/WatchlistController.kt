package com.equilens.portfolio.watchlist

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.security.SecurityRequirement

@RestController
@RequestMapping("/api/watchlist")
@SecurityRequirement(name = "bearerAuth")
class WatchlistController(
    private val watchlistService: WatchlistService
) {

    @GetMapping
    fun getWatchlist(
        authentication: Authentication
    ): List<WatchlistItem> {
        return watchlistService.getWatchlist(authentication.userId())
    }

    @PostMapping("/{companyId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun addToWatchlist(
        authentication: Authentication,
        @PathVariable companyId: Long
    ): WatchlistItem {
        return watchlistService.addToWatchlist(
            userId = authentication.userId(),
            companyId = companyId
        )
    }

    @DeleteMapping("/{companyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeFromWatchlist(
        authentication: Authentication,
        @PathVariable companyId: Long
    ) {
        watchlistService.removeFromWatchlist(
            userId = authentication.userId(),
            companyId = companyId
        )
    }

    private fun Authentication.userId(): Long = (principal as? Number)?.toLong()
        ?: throw IllegalStateException("Authenticated user identity is unavailable")
}
