package com.equilens.backend.financial

import com.equilens.backend.company.Company
import jakarta.persistence.*

@Entity
@Table(
    name = "financial_statements",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_company_fiscal_year",
            columnNames = ["company_id", "fiscal_year"]
        )
    ]
)
class FinancialStatement(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    var company: Company? = null,

    @Column(name = "fiscal_year", nullable = false)
    var fiscalYear: Int = 0,

    @Column(nullable = false)
    var revenue: Double = 0.0,

    @Column(nullable = false)
    var operatingProfit: Double = 0.0,

    @Column(nullable = false)
    var netProfit: Double = 0.0,

    @Column(nullable = false)
    var eps: Double = 0.0,

    @Column(nullable = false)
    var freeCashFlow: Double = 0.0,

    @Column(nullable = false)
    var totalAssets: Double = 0.0,

    @Column(nullable = false)
    var totalDebt: Double = 0.0,

    @Column(nullable = false)
    var equity: Double = 0.0
)