package com.equilens.backend.portfolio

import com.equilens.backend.auth.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/portfolio")
class PortfolioController(
    private val portfolioService: PortfolioService,
    private val userRepository: UserRepository
) {

    @GetMapping
    fun getPortfolio(
        authentication: Authentication
    ): PortfolioSummaryResponse {

        val userId = getUserId(authentication)

        return portfolioService.getSummary(userId)
    }

    @GetMapping("/transactions")
    fun getTransactions(
        authentication: Authentication
    ): List<PortfolioTransaction> {

        val userId = getUserId(authentication)

        return portfolioService.getTransactions(userId)
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    fun addTransaction(
        @RequestBody request: TransactionRequest,
        authentication: Authentication
    ): PortfolioTransaction {

        val userId = getUserId(authentication)

        return portfolioService.addTransaction(
            userId = userId,
            companyId = request.companyId,
            type = request.type,
            quantity = request.quantity,
            price = request.price
        )
    }

    private fun getUserId(
        authentication: Authentication
    ): Long {

        return userRepository
            .findByEmail(authentication.name)
            .orElseThrow {
                IllegalArgumentException(
                    "Authenticated user not found"
                )
            }
            .id!!
    }
}

data class TransactionRequest(
    val companyId: Long,
    val type: TransactionType,
    val quantity: Double,
    val price: Double
)