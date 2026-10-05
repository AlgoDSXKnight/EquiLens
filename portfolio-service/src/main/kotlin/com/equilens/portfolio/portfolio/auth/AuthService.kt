package com.equilens.portfolio.auth

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    fun register(request: RegisterRequest): AuthResponse {

        val email = request.email.trim().lowercase()

        if (email.isBlank()) {
            throw IllegalArgumentException("Email is required")
        }

        if (email.length > 120 || !email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            throw IllegalArgumentException("Enter a valid email address")
        }

        if (request.password.length < 6) {
            throw IllegalArgumentException(
                "Password must contain at least 6 characters"
            )
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "User with this email already exists")
        }

        val name = request.name.trim().ifBlank {
          email.substringBefore("@")
        }

        val user = User(
            email = email,
            name = name,
            password = passwordEncoder.encode(request.password),
            role = "USER"
        )

        val savedUser = userRepository.save(user)

        return AuthResponse(
            token = jwtService.generateToken(savedUser),
            userId = savedUser.id!!,
            email = savedUser.email,
            role = savedUser.role
        )
    }

    fun login(request: LoginRequest): AuthResponse {

        val email = request.email.trim().lowercase()

        val user = userRepository.findByEmailIgnoreCase(email)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password")

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password")
        }

        return AuthResponse(
            token = jwtService.generateToken(user),
            userId = user.id!!,
            email = user.email,
            role = user.role
        )
    }

    fun getUser(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow {
                IllegalArgumentException("User not found")
            }
    }
}
