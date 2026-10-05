package com.equilens.research.research

internal object ResearchMetrics {
    fun calculate(years: List<FinancialYearDto>): ResearchMetricsDto {
        val latest = years.lastOrNull()
        val previous = years.getOrNull(years.lastIndex - 1)
        val first = years.firstOrNull()
        val adjacentPrevious = previous?.takeIf { latest?.fiscalYear == it.fiscalYear + 1 }

        fun growth(current: Double?, prior: Double?): Double? =
            if (current == null || prior == null || !current.isFinite() || !prior.isFinite() || prior <= 0.0) {
                null
            } else {
                (current - prior) / prior * 100.0
            }

        fun ratio(numerator: Double?, denominator: Double?): Double? =
            if (numerator == null || denominator == null || !numerator.isFinite() || !denominator.isFinite() || denominator == 0.0) {
                null
            } else {
                numerator / denominator
            }

        val span = if (first != null && latest != null) latest.fiscalYear - first.fiscalYear else 0
        val cagr = if (
            first != null && latest != null && span > 0 && first.revenue.isFinite() && latest.revenue.isFinite() &&
            first.revenue > 0.0 && latest.revenue >= 0.0
        ) {
            (Math.pow(latest.revenue / first.revenue, 1.0 / span) - 1.0) * 100.0
        } else {
            null
        }

        return ResearchMetricsDto(
            revenueGrowth = growth(latest?.revenue, adjacentPrevious?.revenue),
            profitGrowth = growth(latest?.netProfit, adjacentPrevious?.netProfit),
            epsGrowth = growth(latest?.eps, adjacentPrevious?.eps),
            operatingMargin = ratio(latest?.operatingProfit, latest?.revenue)?.times(100.0),
            netMargin = ratio(latest?.netProfit, latest?.revenue)?.times(100.0),
            roe = latest?.takeIf { it.equity > 0.0 }?.let { ratio(it.netProfit, it.equity)?.times(100.0) },
            debtToEquity = latest?.takeIf { it.equity > 0.0 }?.let { ratio(it.totalDebt, it.equity) },
            fcfMargin = ratio(latest?.freeCashFlow, latest?.revenue)?.times(100.0),
            revenueCagr = cagr
        )
    }
}
