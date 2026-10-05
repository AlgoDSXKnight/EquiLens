package com.equilens.portfolio.auth

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthenticationFilterTest {
    private val jwtService = Mockito.mock(JwtService::class.java)
    private val authService = Mockito.mock(AuthService::class.java)
    private val filter = JwtAuthenticationFilter(jwtService, authService)

    @BeforeEach
    fun clearContextBefore() {
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun clearContextAfter() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `missing bearer token continues without authenticating`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        var continued = false
        val chain = FilterChain { _, _ -> continued = true }

        filter.doFilter(request, response, chain)

        assertTrue(continued)
        assertNull(SecurityContextHolder.getContext().authentication)
        Mockito.verifyNoInteractions(jwtService, authService)
    }

    @Test
    fun `invalid bearer token continues without authenticating`() {
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer invalid-token")
        }
        val response = MockHttpServletResponse()
        var continued = false
        val chain = FilterChain { _, _ -> continued = true }
        Mockito.`when`(jwtService.isValid("invalid-token")).thenReturn(false)

        filter.doFilter(request, response, chain)

        assertTrue(continued)
        assertFalse(response.isCommitted)
        assertNull(SecurityContextHolder.getContext().authentication)
        Mockito.verify(jwtService).isValid("invalid-token")
        Mockito.verifyNoInteractions(authService)
    }

    @Test
    fun `valid bearer token sets the database user identity and role`() {
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer valid-token")
        }
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, _ -> }
        Mockito.`when`(jwtService.isValid("valid-token")).thenReturn(true)
        Mockito.`when`(jwtService.extractUserId("valid-token")).thenReturn(47)
        Mockito.`when`(authService.getUser(47)).thenReturn(
            User(id = 47, email = "investor@example.com", role = "USER")
        )

        filter.doFilter(request, response, chain)

        val authentication = SecurityContextHolder.getContext().authentication
        assertEquals(47L, authentication.principal)
        assertEquals("ROLE_USER", authentication.authorities.single().authority)
        Mockito.verify(authService).getUser(47)
    }
}
