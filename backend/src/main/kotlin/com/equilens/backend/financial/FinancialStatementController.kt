package com.equilens.backend.financial

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/companies/{companyId}/financials")
class FinancialStatementController(
    private val financialStatementService: FinancialStatementService
) {

    @GetMapping
    fun getFinancials(
        @PathVariable companyId: Long
    ): List<FinancialStatement> {
        return financialStatementService.getFinancials(companyId)
    }

    @GetMapping("/{fiscalYear}")
    fun getFinancial(
        @PathVariable companyId: Long,
        @PathVariable fiscalYear: Int
    ): FinancialStatement {
        return financialStatementService.getFinancial(
            companyId,
            fiscalYear
        )
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createFinancial(
        @PathVariable companyId: Long,
        @RequestBody financialStatement: FinancialStatement
    ): FinancialStatement {

        return financialStatementService.createFinancial(
            companyId,
            financialStatement
        )
    }

    @PutMapping("/{fiscalYear}")
    fun updateFinancial(
        @PathVariable companyId: Long,
        @PathVariable fiscalYear: Int,
        @RequestBody financialStatement: FinancialStatement
    ): FinancialStatement {

        return financialStatementService.updateFinancial(
            companyId,
            fiscalYear,
            financialStatement
        )
    }

    @DeleteMapping("/{fiscalYear}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteFinancial(
        @PathVariable companyId: Long,
        @PathVariable fiscalYear: Int
    ) {
        financialStatementService.deleteFinancial(
            companyId,
            fiscalYear
        )
    }
}