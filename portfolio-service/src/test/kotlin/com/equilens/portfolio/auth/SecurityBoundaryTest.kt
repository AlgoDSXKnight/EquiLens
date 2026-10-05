package com.equilens.portfolio.auth

import com.equilens.portfolio.portfolio.PortfolioController
import com.equilens.portfolio.portfolio.PortfolioService
import com.equilens.portfolio.portfolio.PortfolioSummary
import com.equilens.portfolio.portfolio.PortfolioTransaction
import com.equilens.portfolio.watchlist.WatchlistController
import com.equilens.portfolio.watchlist.WatchlistService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [PortfolioController::class, WatchlistController::class])
@Import(SecurityConfig::class, JwtAuthenticationFilter::class)
class SecurityBoundaryTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var portfolioService: PortfolioService

    @MockitoBean
    private lateinit var watchlistService: WatchlistService

    @MockitoBean
    private lateinit var jwtService: JwtService

    @MockitoBean
    private lateinit var authService: AuthService

    @Test
    fun `protected portfolio summary rejects a missing token`() {
        mockMvc.perform(get("/api/portfolio/summary"))
            .andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(portfolioService)
    }

    @Test
    fun `protected portfolio summary rejects an invalid token`() {
        Mockito.`when`(jwtService.isValid("invalid-token")).thenReturn(false)

        mockMvc.perform(
            get("/api/portfolio/summary")
                .header("Authorization", "Bearer invalid-token")
        ).andExpect(status().isUnauthorized)

        Mockito.verifyNoInteractions(authService, portfolioService)
    }

    @Test
    fun `a request body cannot choose the portfolio owner`() {
        val result = mockMvc.perform(
            post("/api/portfolio/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":99,"companyId":1,"type":"BUY","quantity":1,"price":10}""")
        ).andExpect(status().isUnauthorized)
            .andReturn()

        assertEquals(401, result.response.status)
        Mockito.verifyNoInteractions(portfolioService)
    }

    @Test
    fun `portfolio routes use the verified token identity instead of a query user id`() {
        authorizeAs(7)
        Mockito.`when`(portfolioService.getSummary(7)).thenReturn(
            PortfolioSummary(1, 7, "Main Portfolio", 0.0, 0.0, 0.0, 0.0, emptyList())
        )

        mockMvc.perform(
            get("/api/portfolio/summary")
                .header("Authorization", "Bearer user-a-token")
                .param("userId", "99")
        ).andExpect(status().isOk)

        Mockito.verify(portfolioService).getSummary(7)
        Mockito.verify(portfolioService, Mockito.never()).getSummary(99)
    }

    @Test
    fun `watchlist routes use the verified token identity instead of a query user id`() {
        authorizeAs(7)

        mockMvc.perform(
            get("/api/watchlist")
                .header("Authorization", "Bearer user-a-token")
                .param("userId", "99")
        ).andExpect(status().isOk)

        Mockito.verify(watchlistService).getWatchlist(7)
        Mockito.verify(watchlistService, Mockito.never()).getWatchlist(99)
    }

    @Test
    fun `transaction writes use the verified token identity instead of a body user id`() {
        authorizeAs(7)
        Mockito.`when`(portfolioService.addTransaction(7, 1, "BUY", 1.0, 10.0))
            .thenReturn(PortfolioTransaction(portfolioId = 4, companyId = 1, type = "BUY", quantity = 1.0, price = 10.0))

        mockMvc.perform(
            post("/api/portfolio/transactions")
                .header("Authorization", "Bearer user-a-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":99,"companyId":1,"type":"BUY","quantity":1,"price":10}""")
        ).andExpect(status().isCreated)

        Mockito.verify(portfolioService).addTransaction(7, 1, "BUY", 1.0, 10.0)
        Mockito.verify(portfolioService, Mockito.never()).addTransaction(99, 1, "BUY", 1.0, 10.0)
    }

    private fun authorizeAs(userId: Long) {
        Mockito.`when`(jwtService.isValid("user-a-token")).thenReturn(true)
        Mockito.`when`(jwtService.extractUserId("user-a-token")).thenReturn(userId)
        Mockito.`when`(authService.getUser(userId)).thenReturn(
            User(id = userId, email = "investor@example.com", role = "USER")
        )
    }
}
