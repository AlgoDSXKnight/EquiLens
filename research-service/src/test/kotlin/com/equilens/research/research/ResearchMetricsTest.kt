package com.equilens.research.research

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ResearchMetricsTest {
    @Test
    fun `derives annual growth margins ratios and revenue CAGR from consecutive statements`() {
        val result = ResearchMetrics.calculate(
            listOf(
                statement(year = 2024, revenue = 100.0, operatingProfit = 20.0, netProfit = 10.0, eps = 5.0, fcf = 8.0, debt = 30.0, equity = 50.0),
                statement(year = 2025, revenue = 121.0, operatingProfit = 30.25, netProfit = 12.0, eps = 6.0, fcf = 12.1, debt = 24.0, equity = 60.0)
            )
        )

        assertEquals(21.0, result.revenueGrowth!!, 0.000001)
        assertEquals(20.0, result.profitGrowth!!, 0.000001)
        assertEquals(20.0, result.epsGrowth!!, 0.000001)
        assertEquals(25.0, result.operatingMargin!!, 0.000001)
        assertEquals(12.0 / 121.0 * 100.0, result.netMargin!!, 0.000001)
        assertEquals(20.0, result.roe!!, 0.000001)
        assertEquals(0.4, result.debtToEquity!!, 0.000001)
        assertEquals(10.0, result.fcfMargin!!, 0.000001)
        assertEquals(21.0, result.revenueCagr!!, 0.000001)
    }

    @Test
    fun `returns null growth for missing years and non-positive prior values`() {
        val missingYear = ResearchMetrics.calculate(
            listOf(
                statement(year = 2022, revenue = 100.0, netProfit = 10.0, eps = 5.0),
                statement(year = 2024, revenue = 120.0, netProfit = 12.0, eps = 6.0)
            )
        )
        val nonPositivePrior = ResearchMetrics.calculate(
            listOf(
                statement(year = 2024, revenue = 100.0, netProfit = -10.0, eps = -2.0),
                statement(year = 2025, revenue = 120.0, netProfit = 5.0, eps = 1.0)
            )
        )

        assertNull(missingYear.revenueGrowth)
        assertNull(missingYear.profitGrowth)
        assertNull(nonPositivePrior.profitGrowth)
        assertNull(nonPositivePrior.epsGrowth)
    }

    @Test
    fun `returns null for zero denominators and insufficient CAGR history`() {
        val result = ResearchMetrics.calculate(
            listOf(statement(year = 2025, revenue = 0.0, operatingProfit = 10.0, netProfit = 5.0, eps = 1.0, fcf = 4.0, debt = 7.0, equity = 0.0))
        )

        assertNull(result.operatingMargin)
        assertNull(result.netMargin)
        assertNull(result.roe)
        assertNull(result.debtToEquity)
        assertNull(result.fcfMargin)
        assertNull(result.revenueCagr)
    }

    @Test
    fun `does not report ROE or debt equity when book equity is negative`() {
        val result = ResearchMetrics.calculate(
            listOf(statement(year = 2025, revenue = 100.0, netProfit = 5.0, debt = 20.0, equity = -10.0))
        )

        assertNull(result.roe)
        assertNull(result.debtToEquity)
    }

    private fun statement(
        year: Int,
        revenue: Double,
        operatingProfit: Double = 0.0,
        netProfit: Double = 0.0,
        eps: Double = 0.0,
        fcf: Double = 0.0,
        debt: Double = 0.0,
        equity: Double = 0.0
    ) = FinancialYearDto(year, revenue, operatingProfit, netProfit, eps, fcf, totalAssets = 100.0, totalDebt = debt, equity = equity)
}
