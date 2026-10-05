package com.equilens.portfolio.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @param:Value("\${jwt.secret}") private val secret: String,
    @param:Value("\${jwt.expiration-ms:86400000}") private val expirationMs: Long
) {

    init {
        require(secret.toByteArray().size >= 32) { "JWT_SECRET must contain at least 32 bytes" }
        require(expirationMs > 0) { "JWT_EXPIRATION_MS must be positive" }
    }

    private fun signingKey(): SecretKey {
        return Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateToken(user: User): String {
        val now = Date()
        val expiry = Date(now.time + expirationMs)

        return Jwts.builder()
            .subject(user.id!!.toString())
            .claim("userId", user.id)
            .claim("email", user.email)
            .claim("role", user.role)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey())
            .compact()
    }

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
        } catch (ex: Exception) {
            false
        }
    }
}
