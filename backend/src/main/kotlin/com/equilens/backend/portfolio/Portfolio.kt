package com.equilens.backend.portfolio

import com.equilens.backend.auth.User
import jakarta.persistence.*

@Entity
@Table(
    name = "portfolios",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_portfolio_user",
            columnNames = ["user_id"]
        )
    ]
)
class Portfolio(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,

    @Column(nullable = false, length = 120)
    var name: String = "Main Portfolio"
)