package com.equilens.backend.auth

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

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

        return authService.register(
            name = request.name,
            email = request.email,
            password = request.password
        )

    }


    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest
    ): AuthResponse {

        return authService.login(
            email = request.email,
            password = request.password
        )

    }


    @GetMapping("/me")
    fun me(
        authentication: Authentication
    ): User {

        return authService.getUserByEmail(
            authentication.name
        )

    }

}


data class RegisterRequest(

    val name: String = "",

    val email: String = "",

    val password: String = ""

)


data class LoginRequest(

    val email: String = "",

    val password: String = ""

)