package com.equilens.research.auth

import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity
    ): SecurityFilterChain {

        http
            .csrf { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, _ ->
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                    response.contentType = "application/json"
                    response.writer.write(
                        """{"error":"Authentication required"}"""
                    )
                }
            }
            .authorizeHttpRequests {
                it
                    // Write operations require a valid JWT.
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/companies",
                        "/api/companies/*/financials"
                    )
                    .authenticated()

                    .requestMatchers(
                        HttpMethod.PUT,
                        "/api/companies/*",
                        "/api/companies/*/financials/*"
                    )
                    .authenticated()

                    .requestMatchers(
                        HttpMethod.DELETE,
                        "/api/companies/*",
                        "/api/companies/*/financials/*"
                    )
                    .authenticated()

                    // Research is read-only from the user's perspective.
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/**"
                    )
                    .permitAll()

                    .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                    )
                    .permitAll()

                    .anyRequest()
                    .permitAll()
            }
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }
}