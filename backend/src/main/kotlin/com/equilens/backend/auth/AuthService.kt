package com.equilens.backend.auth

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(

    private val userRepository: UserRepository,

    private val passwordEncoder: PasswordEncoder,

    private val jwtService: JwtService

) {

    fun register(
        name: String,
        email: String,
        password: String
    ): AuthResponse {

        val normalizedEmail =
            email.trim().lowercase()


        if (
            name.isBlank() ||
            normalizedEmail.isBlank() ||
            password.isBlank()
        ) {

            throw IllegalArgumentException(
                "Name, email and password are required"
            )

        }


        if (
            password.length < 6
        ) {

            throw IllegalArgumentException(
                "Password must contain at least 6 characters"
            )

        }


        if (
            userRepository.existsByEmail(
                normalizedEmail
            )
        ) {

            throw IllegalArgumentException(
                "Email is already registered"
            )

        }


        val user =
            User(
                name = name.trim(),
                email = normalizedEmail,
                passwordHash =
                    passwordEncoder.encode(password),
                role = "USER"
            )


        val savedUser =
            userRepository.save(user)


        val token =
            jwtService.generateToken(savedUser)


        return AuthResponse(
            token = token,
            userId = savedUser.id!!,
            name = savedUser.name,
            email = savedUser.email,
            role = savedUser.role
        )

    }


    fun login(
        email: String,
        password: String
    ): AuthResponse {

        val normalizedEmail =
            email.trim().lowercase()


        val user =
            userRepository
                .findByEmail(normalizedEmail)
                .orElseThrow {

                    IllegalArgumentException(
                        "Invalid email or password"
                    )

                }


        if (
            !passwordEncoder.matches(
                password,
                user.passwordHash
            )
        ) {

            throw IllegalArgumentException(
                "Invalid email or password"
            )

        }


        val token =
            jwtService.generateToken(user)


        return AuthResponse(
            token = token,
            userId = user.id!!,
            name = user.name,
            email = user.email,
            role = user.role
        )

    }


    fun getUserByEmail(
        email: String
    ): User {

        return userRepository
            .findByEmail(email)
            .orElseThrow {

                IllegalArgumentException(
                    "User not found"
                )

            }

    }

}


data class AuthResponse(

    val token: String,

    val userId: Long,

    val name: String,

    val email: String,

    val role: String

)