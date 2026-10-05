package com.equilens.portfolio.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JwtServiceTest {
    private val secret = "01234567890123456789012345678901"

    @Test
    fun `JWT contains identity claims and validates`() {
        val service = JwtService(secret, 60_000)
        val token = service.generateToken(User(id = 47, email = "investor@example.com", role = "USER"))
        val claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.toByteArray())).build().parseSignedClaims(token).payload

        assertTrue(service.isValid(token))
        assertEquals(47, claims["userId"].toString().toLong())
        assertEquals("investor@example.com", claims["email"])
        assertEquals("USER", claims["role"])
        assertTrue(claims.issuedAt != null)
        assertTrue(claims.expiration != null)
    }

    @Test
    fun `JWT rejects expired and malformed tokens`() {
        val service = JwtService(secret, 1)
        val expired = service.generateToken(User(id = 1, email = "investor@example.com", role = "USER"))
        Thread.sleep(10)
        assertFalse(service.isValid(expired))
        assertFalse(service.isValid("malformed"))
    }
}
