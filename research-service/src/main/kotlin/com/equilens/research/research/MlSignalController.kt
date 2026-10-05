package com.equilens.research.research

import com.equilens.research.company.CompanyRepository
import com.equilens.research.financial.FinancialStatementRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.server.ResponseStatusException
import java.net.http.HttpClient
import java.time.Duration

@RestController
@RequestMapping("/api/research/companies")
class MlSignalController(
    private val companyRepository: CompanyRepository,
    private val financialRepository: FinancialStatementRepository,
    @Value("\${equilens.ml-service.url:http://localhost:8000}") mlServiceUrl: String
) {
    private val requestFactory = JdkClientHttpRequestFactory(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(3)).build()).apply {
        setReadTimeout(Duration.ofSeconds(5))
    }
    private val mlClient = RestClient.builder().baseUrl(mlServiceUrl).requestFactory(requestFactory).build()

    @GetMapping("/{id}/signal")
    fun signal(@PathVariable id: Long): Map<String, Any?> {
        val company = companyRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found")
        }
        val history = financialRepository.findByCompanyIdOrderByFiscalYearAsc(id)
        val latest = history.lastOrNull()
        if (latest == null) throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Financial statements are required to calculate a research signal")
        val financialYears = history.map {
            FinancialYearDto(it.fiscalYear, it.revenue, it.operatingProfit, it.netProfit, it.eps, it.freeCashFlow, it.totalAssets, it.totalDebt, it.equity)
        }
        val metrics = ResearchMetrics.calculate(financialYears)
        val features = mapOf(
            "revenueGrowth" to metrics.revenueGrowth,
            "profitGrowth" to metrics.profitGrowth,
            "roe" to metrics.roe,
            "debtToEquity" to metrics.debtToEquity,
            "peRatio" to company.peRatio.takeIf { it > 0.0 },
            "fcfMargin" to metrics.fcfMargin,
            "operatingMargin" to metrics.operatingMargin,
            "epsGrowth" to metrics.epsGrowth
        )
        val prediction = try {
            mlClient.post().uri("/predict").contentType(MediaType.APPLICATION_JSON).body(features).retrieve().body(Map::class.java)
                ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "ML Service returned an empty prediction")
        } catch (exception: RestClientResponseException) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "ML Service rejected the research signal request (HTTP ${exception.statusCode.value()})", exception)
        } catch (exception: RestClientException) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ML Service is unavailable", exception)
        }
        return mapOf(
            "company" to mapOf("id" to company.id, "symbol" to company.symbol, "name" to company.name),
            "features" to features,
            "model" to prediction
        )
    }
}
