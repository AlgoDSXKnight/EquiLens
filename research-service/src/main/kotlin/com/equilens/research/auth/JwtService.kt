package com.equilens.research.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey

@Service
class JwtService(
    @param:Value("\${jwt.secret}") private val secret: String
) {

    init {
        require(secret.toByteArray(StandardCharsets.UTF_8).size >= 32) {
            "JWT_SECRET must contain at least 32 bytes"
        }
    }

    private fun signingKey(): SecretKey =
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    fun extractUserId(token: String): Long {
        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .payload
            .subject
            .toLong()
    }

    fun isValid(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)

            true
        } catch (_: Exception) {
            false
        }
    }
}