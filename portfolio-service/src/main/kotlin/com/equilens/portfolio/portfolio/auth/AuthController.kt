package com.equilens.portfolio.auth

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.security.SecurityRequirement

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @RequestBody request: RegisterRequest
    ): AuthResponse {
        return authService.register(request)
    }

    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest
    ): AuthResponse {
        return authService.login(request)
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    fun me(
        authentication: Authentication
    ): MeResponse {

        val userId = authentication.principal
            .toString()
            .toLong()

        val user = authService.getUser(userId)

        return MeResponse(
            userId = user.id!!,
            email = user.email,
            role = user.role
        )
    }
}
