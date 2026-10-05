package com.equilens.research.research

import com.equilens.research.company.Company
import com.equilens.research.company.CompanyRepository
import com.equilens.research.financial.FinancialStatement
import com.equilens.research.financial.FinancialStatementRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.Optional
import java.util.concurrent.atomic.AtomicReference

class MlSignalControllerTest {
    @Test
    fun `sends derived features as JSON over HTTP 1 and maps the ML response`() {
        val requestBody = AtomicReference("")
        val contentType = AtomicReference("")
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/predict") { exchange ->
            contentType.set(exchange.requestHeaders.getFirst("Content-Type") ?: "")
            requestBody.set(String(exchange.requestBody.readAllBytes(), StandardCharsets.UTF_8))
            val response = """{"signal":"POSITIVE","probability":0.9,"modelVersion":"test-model","datasetType":"synthetic_demo"}"""
                .toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.start()

        try {
            val companyRepository = Mockito.mock(CompanyRepository::class.java)
            val financialRepository = Mockito.mock(FinancialStatementRepository::class.java)
            Mockito.`when`(companyRepository.findById(1L)).thenReturn(
                Optional.of(Company(id = 1, symbol = "TEST", name = "Test Co", sector = "Technology", peRatio = 20.0))
            )
            Mockito.`when`(financialRepository.findByCompanyIdOrderByFiscalYearAsc(1L)).thenReturn(
                listOf(
                    statement(2024, 100.0, 10.0, 5.0, 2.0, 8.0, 4.0, 20.0),
                    statement(2025, 121.0, 15.0, 6.0, 2.4, 12.1, 5.0, 30.0)
                )
            )

            val controller = MlSignalController(
                companyRepository,
                financialRepository,
                "http://127.0.0.1:${server.address.port}"
            )
            val response = controller.signal(1L)
            val features = ObjectMapper().readValue(requestBody.get(), Map::class.java)
            val model = response["model"] as Map<*, *>

            assertTrue(contentType.get().startsWith("application/json"))
            assertEquals(21.0, (features["revenueGrowth"] as Number).toDouble(), 0.000001)
            assertEquals(20.0, (features["profitGrowth"] as Number).toDouble(), 0.000001)
            assertEquals(20.0, (features["roe"] as Number).toDouble(), 0.000001)
            assertEquals(5.0 / 30.0, (features["debtToEquity"] as Number).toDouble(), 0.000001)
            assertEquals(20.0, (features["peRatio"] as Number).toDouble(), 0.000001)
            assertEquals(12.1 / 121.0 * 100.0, (features["fcfMargin"] as Number).toDouble(), 0.000001)
            assertEquals(15.0 / 121.0 * 100.0, (features["operatingMargin"] as Number).toDouble(), 0.000001)
            assertEquals(20.0, (features["epsGrowth"] as Number).toDouble(), 0.000001)
            assertEquals("POSITIVE", model["signal"])
            assertEquals("TEST", (response["company"] as Map<*, *>)["symbol"])
        } finally {
            server.stop(0)
        }
    }

    private fun statement(
        year: Int,
        revenue: Double,
        operatingProfit: Double,
        netProfit: Double,
        eps: Double,
        freeCashFlow: Double,
        debt: Double,
        equity: Double
    ) = FinancialStatement(
        fiscalYear = year,
        revenue = revenue,
        operatingProfit = operatingProfit,
        netProfit = netProfit,
        eps = eps,
        freeCashFlow = freeCashFlow,
        totalAssets = 100.0,
        totalDebt = debt,
        equity = equity
    )
}
