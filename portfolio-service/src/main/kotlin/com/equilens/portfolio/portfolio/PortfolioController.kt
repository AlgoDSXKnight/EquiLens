package com.equilens.portfolio.portfolio

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestBody
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/portfolio")
@SecurityRequirement(name = "bearerAuth")
class PortfolioController(
    private val portfolioService: PortfolioService
) {

    @GetMapping
    fun getPortfolio(
        authentication: Authentication
    ): Portfolio {
        return portfolioService.getOrCreatePortfolio(authentication.userId())
    }

    @GetMapping("/transactions")
    fun getTransactions(
        authentication: Authentication
    ): List<PortfolioTransaction> {
        return portfolioService.getTransactions(authentication.userId())
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    fun addTransaction(
        authentication: Authentication,
        @RequestBody request: CreateTransactionRequest
    ): PortfolioTransaction {
        return portfolioService.addTransaction(
            userId = authentication.userId(),
            companyId = request.companyId,
            type = request.type,
            quantity = request.quantity,
            price = request.price
        )
    }

    @GetMapping("/summary")
    fun getSummary(
        authentication: Authentication
    ): PortfolioSummary {
        return portfolioService.getSummary(authentication.userId())
    }

    private fun Authentication.userId(): Long = (principal as? Number)?.toLong()
        ?: throw IllegalStateException("Authenticated user identity is unavailable")
}

data class CreateTransactionRequest(
    val companyId: Long,
    val type: String,
    val quantity: Double,
    val price: Double
)
