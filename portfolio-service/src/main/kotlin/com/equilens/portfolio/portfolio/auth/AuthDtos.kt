package com.equilens.portfolio.auth

data class RegisterRequest(
    val email: String = "",
    val password: String = "",
    val name: String = ""
)

data class LoginRequest(
    val email: String = "",
    val password: String = ""
)

data class AuthResponse(
    val token: String,
    val userId: Long,
    val email: String,
    val role: String
)

data class MeResponse(
    val userId: Long,
    val email: String,
    val role: String
)