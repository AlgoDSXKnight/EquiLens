package com.equilens.portfolio.watchlist

import com.equilens.portfolio.research.ResearchClient
import com.equilens.portfolio.research.ResearchCompany
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class WatchlistServiceTest {
    private val repository = Mockito.mock(WatchlistRepository::class.java)
    private val research = Mockito.mock(ResearchClient::class.java)
    private val service = WatchlistService(repository, research)

    @Test
    fun `duplicate watchlist entry returns conflict`() {
        Mockito.`when`(research.getCompany(4)).thenReturn(ResearchCompany(id = 4))
        Mockito.`when`(repository.findByUserIdAndCompanyId(8, 4)).thenReturn(WatchlistItem(id = 2, userId = 8, companyId = 4))

        val error = assertThrows(ResponseStatusException::class.java) {
            service.addToWatchlist(userId = 8, companyId = 4)
        }

        assertEquals(HttpStatus.CONFLICT, error.statusCode)
        Mockito.verify(repository, Mockito.never()).save(Mockito.any(WatchlistItem::class.java))
    }

    @Test
    fun `removing an absent watchlist entry returns not found`() {
        Mockito.`when`(repository.findByUserIdAndCompanyId(8, 4)).thenReturn(null)

        val error = assertThrows(ResponseStatusException::class.java) {
            service.removeFromWatchlist(userId = 8, companyId = 4)
        }

        assertEquals(HttpStatus.NOT_FOUND, error.statusCode)
        Mockito.verify(repository, Mockito.never()).delete(Mockito.any(WatchlistItem::class.java))
    }
}
