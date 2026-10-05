package com.equilens.research.company

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/companies")
class CompanyController(
    private val companyService: CompanyService
) {

    @GetMapping
    fun getAllCompanies(): List<Company> {
        return companyService.getAllCompanies()
    }

    @GetMapping("/{id}")
    fun getCompany(
        @PathVariable id: Long
    ): Company {
        return companyService.getCompany(id)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCompany(
        @RequestBody company: Company
    ): Company {
        return companyService.createCompany(company)
    }

    @PutMapping("/{id}")
    fun updateCompany(
        @PathVariable id: Long,
        @RequestBody company: Company
    ): Company {
        return companyService.updateCompany(id, company)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCompany(
        @PathVariable id: Long
    ) {
        companyService.deleteCompany(id)
    }
}