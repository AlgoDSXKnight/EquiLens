package com.equilens.research.valuation

import com.equilens.research.company.CompanyRepository
import com.equilens.research.financial.FinancialStatementRepository
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

data class DcfRequest(val freeCashFlow: Double, val annualGrowthRate: Double, val projectionYears: Int, val wacc: Double, val terminalGrowthRate: Double)
data class DcfYear(val year: Int, val projectedFreeCashFlow: Double, val discountFactor: Double, val presentValue: Double)
data class DcfAssumptions(val freeCashFlow: Double, val annualGrowthRate: Double, val projectionYears: Int, val wacc: Double, val terminalGrowthRate: Double)
data class DcfResult(val assumptions: DcfAssumptions, val projections: List<DcfYear>, val terminalValue: Double, val presentValueOfTerminalValue: Double, val enterpriseValue: Double)
data class RelativeValuationRequest(val companyId: Long, val peerPe: Double? = null, val peerPb: Double? = null)
data class RelativeMetric(val multiple: String, val companyValue: Double?, val referenceValue: Double?, val impliedMarketCap: Double?, val available: Boolean, val note: String?)
data class RelativeValuationResult(val symbol: String, val metrics: List<RelativeMetric>)

@RestController
@RequestMapping("/api/valuation")
class ValuationController(
    private val companyRepository: CompanyRepository,
    private val financialRepository: FinancialStatementRepository
) {
    @PostMapping("/dcf")
    fun dcf(@RequestBody request: DcfRequest): DcfResult {
        if (!request.freeCashFlow.isFinite() || !request.annualGrowthRate.isFinite() || !request.wacc.isFinite() || !request.terminalGrowthRate.isFinite()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Valuation inputs must be finite numbers")
        }
        if (request.projectionYears !in 1..10 || request.annualGrowthRate <= -100.0 || request.annualGrowthRate > 200.0 || request.wacc <= 0.0 || request.wacc > 100.0 || request.wacc <= request.terminalGrowthRate || request.terminalGrowthRate <= -100.0 || request.terminalGrowthRate > 100.0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Use 1–10 years, growth above -100%, WACC in (0, 100], and WACC greater than terminal growth")
        }
        val years = (1..request.projectionYears).map { year ->
            val projected = request.freeCashFlow * Math.pow(1 + request.annualGrowthRate / 100.0, year.toDouble())
            val discount = 1.0 / Math.pow(1 + request.wacc / 100.0, year.toDouble())
            DcfYear(year, projected, discount, projected * discount)
        }
        val finalCashFlow = years.last().projectedFreeCashFlow
        val terminalValue = finalCashFlow * (1 + request.terminalGrowthRate / 100.0) /
            ((request.wacc - request.terminalGrowthRate) / 100.0)
        val pvTerminal = terminalValue * years.last().discountFactor
        val enterpriseValue = years.sumOf { it.presentValue } + pvTerminal
        if (years.any { !it.projectedFreeCashFlow.isFinite() || !it.discountFactor.isFinite() || !it.presentValue.isFinite() } ||
            !terminalValue.isFinite() || !pvTerminal.isFinite() || !enterpriseValue.isFinite()
        ) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "DCF inputs produce values outside the supported numeric range")
        }
        return DcfResult(DcfAssumptions(request.freeCashFlow, request.annualGrowthRate, request.projectionYears, request.wacc, request.terminalGrowthRate), years, terminalValue, pvTerminal, enterpriseValue)
    }

    @PostMapping("/relative")
    fun relative(@RequestBody request: RelativeValuationRequest): RelativeValuationResult {
        if (listOfNotNull(request.peerPe, request.peerPb).any { !it.isFinite() || it <= 0.0 }) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Reference valuation multiples must be finite positive numbers")
        }
        val company = companyRepository.findById(request.companyId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found")
        }
        val latest = financialRepository.findByCompanyIdOrderByFiscalYearAsc(request.companyId).lastOrNull()
        val marketCap = company.marketCap.takeIf { it > 0 }
        val ownPe = company.peRatio.takeIf { it > 0 }
        val ownPb = if (latest != null && latest.equity > 0 && marketCap != null) marketCap / latest.equity else null
        val impliedPeMarketCap = if (ownPe != null && request.peerPe != null && request.peerPe > 0 && marketCap != null) marketCap * request.peerPe / ownPe else null
        val impliedPbMarketCap = if (ownPb != null && request.peerPb != null && request.peerPb > 0 && latest != null) latest.equity * request.peerPb else null
        return RelativeValuationResult(company.symbol, listOf(
            RelativeMetric("P/E", ownPe, request.peerPe, impliedPeMarketCap, ownPe != null, if (ownPe == null) "Company P/E is unavailable or non-positive" else null),
            RelativeMetric("P/B", ownPb, request.peerPb, impliedPbMarketCap, ownPb != null, if (ownPb == null) "Positive reported book equity is required" else null),
            RelativeMetric("EV/EBITDA", null, null, null, false, "Cash and depreciation/amortization data are not stored, so EV/EBITDA cannot be calculated reliably")
        ))
    }
}
