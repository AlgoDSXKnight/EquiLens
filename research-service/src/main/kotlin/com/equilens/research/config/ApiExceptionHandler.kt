package com.equilens.research.config

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import org.slf4j.LoggerFactory
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun malformedBody() = ResponseEntity.badRequest().body(mapOf("error" to "Request body is missing or malformed"))

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun unsupportedMethod() = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(mapOf("error" to "HTTP method is not supported for this endpoint"))

    @ExceptionHandler(NoResourceFoundException::class)
    fun missingRoute() = ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Endpoint not found"))

    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(exception: IllegalArgumentException) =
        ResponseEntity.badRequest().body(mapOf("error" to (exception.message ?: "Invalid request")))

    @ExceptionHandler(ResponseStatusException::class)
    fun status(exception: ResponseStatusException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(exception.statusCode).body(mapOf("error" to (exception.reason ?: "Request failed")))

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun conflict() = ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "The request conflicts with existing data"))

    @ExceptionHandler(Exception::class)
    fun internalError(exception: Exception): ResponseEntity<Map<String, String>> {
        logger.error("Unhandled API exception", exception)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Internal server error"))
    }
}
