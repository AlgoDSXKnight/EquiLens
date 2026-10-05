package com.equilens.portfolio.research

import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import org.springframework.http.client.JdkClientHttpRequestFactory
import java.net.http.HttpClient
import java.time.Duration

data class ResearchCompany(
    val id: Long? = null,
    val symbol: String = "",
    val name: String = "",
    val sector: String = "",
    val currentPrice: Double = 0.0,
    val marketCap: Double = 0.0,
    val revenueGrowth: Double = 0.0,
    val profitGrowth: Double = 0.0,
    val roe: Double = 0.0,
    val debtToEquity: Double = 0.0,
    val peRatio: Double = 0.0
)

@Service
class ResearchClient(
    @Value("\${equilens.research-service.url:http://localhost:8080}") baseUrl: String
) {

    private val requestFactory = JdkClientHttpRequestFactory(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(3)).build()).apply {
        setReadTimeout(Duration.ofSeconds(5))
    }
    private val restClient = RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(requestFactory)
        .build()

    fun getCompany(companyId: Long): ResearchCompany {
        try {
            return restClient.get()
                .uri("/api/companies/{id}", companyId)
                .retrieve()
                .body(ResearchCompany::class.java)
                ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Research Service returned no company data")
        } catch (exception: RestClientResponseException) {
            val status = if (exception.statusCode == HttpStatus.NOT_FOUND) HttpStatus.NOT_FOUND else HttpStatus.BAD_GATEWAY
            throw ResponseStatusException(status, if (status == HttpStatus.NOT_FOUND) "Company not found in Research Service" else "Research Service returned an error", exception)
        } catch (exception: RestClientException) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Research Service is unavailable", exception)
        }
    }
}
