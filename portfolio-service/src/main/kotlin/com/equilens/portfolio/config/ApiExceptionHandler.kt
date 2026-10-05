package com.equilens.portfolio.config

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun malformedBody(): org.springframework.http.ResponseEntity<Map<String, String>> =
        org.springframework.http.ResponseEntity.badRequest().body(mapOf("error" to "Request body is missing or malformed"))

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun unsupportedMethod(): org.springframework.http.ResponseEntity<Map<String, String>> =
        org.springframework.http.ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(mapOf("error" to "HTTP method is not supported for this endpoint"))

    @ExceptionHandler(NoResourceFoundException::class)
    fun missingRoute(): org.springframework.http.ResponseEntity<Map<String, String>> =
        org.springframework.http.ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Endpoint not found"))


    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(exception: ResponseStatusException): org.springframework.http.ResponseEntity<Map<String, String>> =
        org.springframework.http.ResponseEntity.status(exception.statusCode)
            .body(mapOf("error" to (exception.reason ?: "Request failed")))

    @ExceptionHandler(DataIntegrityViolationException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflict(): Map<String, String> = mapOf("error" to "The request conflicts with existing data")

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(
        exception: IllegalArgumentException
    ): Map<String, String> {
        return mapOf(
            "error" to (exception.message ?: "Invalid request")
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        exception: Exception
    ): org.springframework.http.ResponseEntity<Map<String, String>> {
        logger.error("Unhandled API exception", exception)
        return org.springframework.http.ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("error" to "Internal server error"))
    }
}
