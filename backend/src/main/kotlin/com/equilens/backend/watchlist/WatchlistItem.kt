package com.equilens.backend.watchlist

import com.equilens.backend.auth.User
import com.equilens.backend.company.Company
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false
    )
    var user: User? = null,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(
        name = "company_id",
        nullable = false
    )
    var company: Company? = null
)