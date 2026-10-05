package com.equilens.portfolio.portfolio

import jakarta.persistence.*

@Entity
@Table(name = "portfolios")
class Portfolio(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(nullable = false)
    var name: String = "Main Portfolio"
)