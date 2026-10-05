package com.equilens.backend.company

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/companies")
@CrossOrigin(origins = ["http://localhost:5173"])
class CompanyController(private val service: CompanyService) {
    @GetMapping fun all() = service.findAll()
    @GetMapping("/{id}") fun byId(@PathVariable id: Long) = service.findById(id)
    @GetMapping("/symbol/{symbol}") fun bySymbol(@PathVariable symbol: String) = service.findBySymbol(symbol)
    @PostMapping @ResponseStatus(HttpStatus.CREATED) fun create(@RequestBody company: Company) = service.create(company)
    @PutMapping("/{id}") fun update(@PathVariable id: Long, @RequestBody company: Company) = service.update(id, company)
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) fun delete(@PathVariable id: Long) = service.delete(id)
}
