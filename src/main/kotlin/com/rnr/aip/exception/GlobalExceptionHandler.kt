package com.rnr.aip.exception

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MultipartException
import org.springframework.web.multipart.support.MissingServletRequestPartException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger: Logger = LogManager.getLogger()

    @ExceptionHandler(EmptyFileException::class)
    fun handleEmptyFileException(e: EmptyFileException): ResponseEntity<ErrorResponse> {
        logger.warn("Empty file uploaded: ${e.message}")
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse("EMPTY_FILE", e.message ?: "File cannot be empty"))
    }

    @ExceptionHandler(MissingServletRequestPartException::class)
    fun handleMissingFile(e: MissingServletRequestPartException): ResponseEntity<ErrorResponse> {
        logger.warn("Missing file parameter: ${e.message}")
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse("MISSING_FILE", "File parameter is required"))
    }

    @ExceptionHandler(MultipartException::class)
    fun handleMultipartException(e: MultipartException): ResponseEntity<ErrorResponse> {
        logger.warn("Multipart request error: ${e.message}")
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse("INVALID_FILE", "Invalid file upload"))
    }

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingHeader(e: MissingRequestHeaderException): ResponseEntity<ErrorResponse> {
        logger.warn("Missing required header: ${e.headerName}")
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse("MISSING_TOKEN", "MIP token is required"))
    }

    @ExceptionHandler(InvalidMipTokenException::class)
    fun handleInvalidMipToken(e: InvalidMipTokenException): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid MIP token: ${e.message}")
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse("INVALID_TOKEN", e.message ?: "Invalid MIP token"))
    }

    @ExceptionHandler(DecryptionException::class)
    fun handleDecryptionException(e: DecryptionException): ResponseEntity<ErrorResponse> {
        logger.error("Decryption failed: ${e.message}", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("DECRYPTION_FAILED", "Failed to decrypt file"))
    }

    @ExceptionHandler(FileProtectionCheckException::class)
    fun handleFileProtectionCheckException(e: FileProtectionCheckException): ResponseEntity<ErrorResponse> {
        logger.error("File protection check failed: ${e.message}", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("PROTECTION_CHECK_FAILED", "Failed to check file protection status"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
    }
}

data class ErrorResponse(
    val code: String,
    val message: String
)

// Custom exceptions
class EmptyFileException(message: String) : RuntimeException(message)
class InvalidMipTokenException(message: String) : RuntimeException(message)
class DecryptionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class FileProtectionCheckException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)