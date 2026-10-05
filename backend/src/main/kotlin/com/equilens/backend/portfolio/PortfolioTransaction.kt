package com.equilens.backend.portfolio

import com.equilens.backend.company.Company
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "portfolio_transactions")
class PortfolioTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    var portfolio: Portfolio? = null,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    var company: Company? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var type: TransactionType = TransactionType.BUY,

    @Column(nullable = false)
    var quantity: Double = 0.0,

    @Column(nullable = false)
    var price: Double = 0.0,

    @Column(nullable = false)
    var transactionDate: LocalDateTime = LocalDateTime.now()
)

enum class TransactionType {
    BUY,
    SELL
}