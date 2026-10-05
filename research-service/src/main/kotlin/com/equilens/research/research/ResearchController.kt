package com.equilens.research.research

import com.equilens.research.company.CompanyRepository
import com.equilens.research.financial.FinancialStatementRepository
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

data class CompanyProfileDto(
    val id: Long,
    val symbol: String,
    val name: String,
    val sector: String,
    val currentPrice: Double,
    val marketCap: Double,
    val peRatio: Double,
    val dataClassification: String?
)

data class FinancialYearDto(
    val fiscalYear: Int,
    val revenue: Double,
    val operatingProfit: Double,
    val netProfit: Double,
    val eps: Double,
    val freeCashFlow: Double,
    val totalAssets: Double,
    val totalDebt: Double,
    val equity: Double
)

data class ResearchMetricsDto(
    val revenueGrowth: Double?,
    val profitGrowth: Double?,
    val epsGrowth: Double?,
    val operatingMargin: Double?,
    val netMargin: Double?,
    val roe: Double?,
    val debtToEquity: Double?,
    val fcfMargin: Double?,
    val revenueCagr: Double?
)

data class CompanyResearchDto(
    val company: CompanyProfileDto,
    val latestFinancials: FinancialYearDto?,
    val historicalFinancials: List<FinancialYearDto>,
    val metrics: ResearchMetricsDto
)

@RestController
@RequestMapping("/api/research/companies")
class ResearchController(
    private val companyRepository: CompanyRepository,
    private val financialRepository: FinancialStatementRepository
) {
    @GetMapping("/{id}")
    fun getResearch(@PathVariable id: Long): CompanyResearchDto {
        val company = companyRepository.findById(id).orElseThrow {
            org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found")
        }
        val years = financialRepository.findByCompanyIdOrderByFiscalYearAsc(id)
            .map { FinancialYearDto(it.fiscalYear, it.revenue, it.operatingProfit, it.netProfit, it.eps, it.freeCashFlow, it.totalAssets, it.totalDebt, it.equity) }
        val latest = years.lastOrNull()
        return CompanyResearchDto(
            CompanyProfileDto(company.id!!, company.symbol, company.name, company.sector, company.currentPrice, company.marketCap, company.peRatio, company.dataClassification),
            latest,
            years,
            ResearchMetrics.calculate(years)
        )
    }
}
