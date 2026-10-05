package com.equilens.research.company

import jakarta.persistence.*

@Entity
@Table(name = "companies")
class Company(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 20)
    var symbol: String = "",

    @Column(nullable = false, length = 120)
    var name: String = "",

    @Column(nullable = false, length = 80)
    var sector: String = "",

    var currentPrice: Double = 0.0,
    var marketCap: Double = 0.0,
    var revenueGrowth: Double = 0.0,
    var profitGrowth: Double = 0.0,
    var roe: Double = 0.0,
    var debtToEquity: Double = 0.0,
    var peRatio: Double = 0.0,

    @Column(name = "data_classification", length = 24)
    var dataClassification: String? = "UNVERIFIED"
)
