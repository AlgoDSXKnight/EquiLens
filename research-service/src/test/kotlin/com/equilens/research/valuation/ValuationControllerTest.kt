package com.equilens.research.valuation

import com.equilens.research.company.CompanyRepository
import com.equilens.research.financial.FinancialStatementRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import org.mockito.Mockito

class ValuationControllerTest {
    private val controller = ValuationController(
        Mockito.mock(CompanyRepository::class.java),
        Mockito.mock(FinancialStatementRepository::class.java)
    )

    @Test
    fun `DCF discounts explicit cash flow projections and terminal value`() {
        val result = controller.dcf(DcfRequest(100.0, 0.0, 2, 10.0, 2.0))

        assertEquals(100.0, result.projections[0].projectedFreeCashFlow)
        assertEquals(90.9090909, result.projections[0].presentValue, 0.000001)
        assertEquals(1275.0, result.terminalValue, 0.000001)
        assertEquals(result.projections.sumOf { it.presentValue } + result.presentValueOfTerminalValue, result.enterpriseValue, 0.000001)
    }

    @Test
    fun `DCF rejects terminal growth at or above WACC`() {
        assertThrows(ResponseStatusException::class.java) {
            controller.dcf(DcfRequest(100.0, 5.0, 5, 8.0, 8.0))
        }
    }

    @Test
    fun `DCF rejects inputs that overflow calculated outputs`() {
        assertThrows(ResponseStatusException::class.java) {
            controller.dcf(DcfRequest(Double.MAX_VALUE, 200.0, 10, 0.01, 0.0))
        }
    }
}
