package com.equilens.portfolio.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.server.ResponseStatusException

class AuthServiceTest {
    private val repository = Mockito.mock(UserRepository::class.java)
    private val encoder = BCryptPasswordEncoder()
    private val service = AuthService(repository, encoder, JwtService("01234567890123456789012345678901", 60_000))

    @Test
    fun `registration normalizes email and stores a BCrypt password`() {
        Mockito.`when`(repository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false)
        Mockito.`when`(repository.save(any(User::class.java))).thenAnswer { invocation ->
            invocation.getArgument<User>(0).apply { id = 9L }
        }

        val response = service.register(RegisterRequest(" User@Example.com ", "correct-horse"))
        val savedUser = ArgumentCaptor.forClass(User::class.java)
        Mockito.verify(repository).save(savedUser.capture())
        val saved = savedUser.value

        assertEquals("user@example.com", response.email)
        assertNotEquals("correct-horse", saved.password)
        assertTrue(encoder.matches("correct-horse", saved.password))
        assertTrue(JwtService("01234567890123456789012345678901", 60_000).isValid(response.token))
    }

    @Test
    fun `duplicate registration returns conflict`() {
        Mockito.`when`(repository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true)

        val error = assertThrows(ResponseStatusException::class.java) {
            service.register(RegisterRequest("user@example.com", "correct-horse"))
        }
        assertEquals(HttpStatus.CONFLICT, error.statusCode)
    }

    @Test
    fun `wrong password returns unauthorized`() {
        val user = User(id = 2, email = "user@example.com", password = encoder.encode("correct-horse"))
        Mockito.`when`(repository.findByEmailIgnoreCase("user@example.com")).thenReturn(user)

        val error = assertThrows(ResponseStatusException::class.java) {
            service.login(LoginRequest("user@example.com", "incorrect"))
        }
        assertEquals(HttpStatus.UNAUTHORIZED, error.statusCode)
    }
}
