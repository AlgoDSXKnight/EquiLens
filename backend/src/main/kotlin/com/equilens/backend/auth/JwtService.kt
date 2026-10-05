package com.equilens.backend.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(

    @Value("\${equilens.jwt.secret}")
    private val secret: String,

    @Value("\${equilens.jwt.expiration}")
    private val expiration: Long

) {

    private fun signingKey(): SecretKey {

        return Keys.hmacShaKeyFor(
            secret.toByteArray(StandardCharsets.UTF_8)
        )

    }


    fun generateToken(user: User): String {

        val now = Date()

        val expiry =
            Date(
                now.time + expiration
            )

        return Jwts.builder()
            .subject(user.email)
            .claim("userId", user.id)
            .claim("name", user.name)
            .claim("role", user.role)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey())
            .compact()

    }


    fun extractEmail(token: String): String {

        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .payload
            .subject

    }


    fun isValid(token: String): Boolean {

        return try {

            Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)

            true

        } catch (exception: Exception) {

            false

        }

    }

}