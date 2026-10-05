package com.equilens.backend.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(

    private val jwtService: JwtService,
    private val userRepository: UserRepository

) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {

        val authorizationHeader =
            request.getHeader("Authorization")


        if (
            authorizationHeader == null ||
            !authorizationHeader.startsWith("Bearer ")
        ) {

            filterChain.doFilter(
                request,
                response
            )

            return
        }


        val token =
            authorizationHeader.substring(7)


        try {

            if (
                jwtService.isValid(token) &&
                SecurityContextHolder
                    .getContext()
                    .authentication == null
            ) {

                val email =
                    jwtService.extractEmail(token)


                val user =
                    userRepository
                        .findByEmail(email)
                        .orElse(null)


                if (user != null) {

                    val authentication =
                        UsernamePasswordAuthenticationToken(
                            user.email,
                            null,
                            emptyList()
                        )


                    authentication.details =
                        WebAuthenticationDetailsSource()
                            .buildDetails(request)


                    SecurityContextHolder
                        .getContext()
                        .authentication =
                        authentication

                }

            }

        } catch (exception: Exception) {

            // Invalid token.
            // The security chain will handle authorization.

        }


        filterChain.doFilter(
            request,
            response
        )

    }

}