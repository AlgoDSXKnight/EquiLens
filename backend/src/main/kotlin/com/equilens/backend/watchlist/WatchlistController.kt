package com.equilens.backend.watchlist

import com.equilens.backend.company.Company
import com.equilens.backend.auth.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/watchlist")
class WatchlistController(

    private val watchlistService:
        WatchlistService,

    private val userRepository:
        UserRepository

) {

    @GetMapping
    fun getWatchlist(
        authentication: Authentication
    ): List<Company> {

        val userId =
            getUserId(authentication)

        return watchlistService
            .getWatchlist(userId)
    }


    @PostMapping("/{companyId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun addToWatchlist(
        @PathVariable companyId: Long,
        authentication: Authentication
    ): Company {

        val userId =
            getUserId(authentication)

        return watchlistService
            .addToWatchlist(
                userId,
                companyId
            )
    }


    @DeleteMapping("/{companyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeFromWatchlist(
        @PathVariable companyId: Long,
        authentication: Authentication
    ) {

        val userId =
            getUserId(authentication)

        watchlistService
            .removeFromWatchlist(
                userId,
                companyId
            )
    }


    @GetMapping("/{companyId}/status")
    fun watchlistStatus(
        @PathVariable companyId: Long,
        authentication: Authentication
    ): WatchlistStatusResponse {

        val userId =
            getUserId(authentication)

        return WatchlistStatusResponse(
            companyId = companyId,
            inWatchlist =
                watchlistService.isInWatchlist(
                    userId,
                    companyId
                )
        )
    }


    private fun getUserId(
        authentication: Authentication
    ): Long {

        val email =
            authentication.name

        return userRepository
            .findByEmail(email)
            .orElseThrow {
                IllegalArgumentException(
                    "Authenticated user not found"
                )
            }
            .id!!
    }
}


data class WatchlistStatusResponse(
    val companyId: Long,
    val inWatchlist: Boolean
)