package com.equilens.portfolio.portfolio

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "portfolio_transactions")
class PortfolioTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "portfolio_id", nullable = false)
    var portfolioId: Long = 0,

    @Column(name = "company_id", nullable = false)
    var companyId: Long = 0,

    @Column(nullable = false, length = 10)
    var type: String = "",

    @Column(nullable = false)
    var quantity: Double = 0.0,

    @Column(nullable = false)
    var price: Double = 0.0,

    @Column(name = "transaction_date", nullable = false)
    var transactionDate: LocalDateTime = LocalDateTime.now()
)