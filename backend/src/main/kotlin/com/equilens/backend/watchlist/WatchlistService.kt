package com.equilens.backend.watchlist

import com.equilens.backend.auth.UserRepository
import com.equilens.backend.company.Company
import com.equilens.backend.company.CompanyRepository
import org.springframework.stereotype.Service

@Service
class WatchlistService(

    private val watchlistRepository:
        WatchlistRepository,

    private val userRepository:
        UserRepository,

    private val companyRepository:
        CompanyRepository

) {

    fun getWatchlist(
        userId: Long
    ): List<Company> {

        return watchlistRepository
            .findByUserIdOrderByIdDesc(userId)
            .mapNotNull { item ->
                item.company
            }
    }


    fun addToWatchlist(
        userId: Long,
        companyId: Long
    ): Company {

        val user =
            userRepository.findById(userId)
                .orElseThrow {
                    IllegalArgumentException(
                        "User not found"
                    )
                }


        val company =
            companyRepository.findById(companyId)
                .orElseThrow {
                    IllegalArgumentException(
                        "Company not found"
                    )
                }


        if (
            watchlistRepository
                .existsByUserIdAndCompanyId(
                    userId,
                    companyId
                )
        ) {

            throw IllegalArgumentException(
                "Company is already in your watchlist"
            )

        }


        val watchlistItem =
            WatchlistItem(
                user = user,
                company = company
            )


        watchlistRepository.save(
            watchlistItem
        )


        return company
    }


    fun removeFromWatchlist(
        userId: Long,
        companyId: Long
    ) {

        val existing =
            watchlistRepository
                .findByUserIdAndCompanyId(
                    userId,
                    companyId
                )
                ?: throw IllegalArgumentException(
                    "Company is not in your watchlist"
                )


        watchlistRepository.delete(
            existing
        )
    }


    fun isInWatchlist(
        userId: Long,
        companyId: Long
    ): Boolean {

        return watchlistRepository
            .existsByUserIdAndCompanyId(
                userId,
                companyId
            )
    }
}