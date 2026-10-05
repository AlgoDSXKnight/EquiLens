package com.equilens.portfolio.watchlist

import jakarta.persistence.*

@Entity
@Table(
    name = "watchlist_items",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_company_watchlist",
            columnNames = ["user_id", "company_id"]
        )
    ]
)
class WatchlistItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(name = "company_id", nullable = false)
    var companyId: Long = 0
)