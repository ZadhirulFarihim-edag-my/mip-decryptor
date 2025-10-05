package com.rnr.aip.service.decryption

import com.rnr.aip.exception.EmptyFileException
import com.rnr.aip.exception.FileProtectionCheckException
import com.rnr.aip.exception.InvalidMipTokenException
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class FileValidator {

    fun validateMipToken(token: String) {
        if (token.isBlank()) {
            throw InvalidMipTokenException("MIP token cannot be empty")
        }
    }

    fun validateInputStream(inputStream: InputStream) {
        try {
            if (inputStream.available() == 0) {
                throw EmptyFileException("File is empty")
            }
        } catch (e: EmptyFileException) {
            throw e
        } catch (e: Exception) {
            throw FileProtectionCheckException("Failed to read input stream", e)
        }
    }
}