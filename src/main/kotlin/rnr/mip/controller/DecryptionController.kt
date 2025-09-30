package rnr.mip.controller

import com.sun.org.slf4j.internal.LoggerFactory
import rnr.mip.service.MipDecryptionService

@RestController
@RequestMapping("/api")
class DecryptionController(
    private val mipDecryptionService: MipDecryptionService
) {

    private val logger = LoggerFactory.getLogger(DecryptionController::class.java)

    @PostMapping("/decrypt")
    fun decryptFile(
        @RequestParam("file") file: MultipartFile,
        @RequestHeader("Authorization") authorizationHeader: String
    ): ResponseEntity<ByteArray> {
        if (file.isEmpty) {
            logger.warn("Received an empty file upload.")
            return ResponseEntity.badRequest().build()
        }

        val token = authorizationHeader.removePrefix("Bearer ").trim()
        if (token.isEmpty()) {
            logger.warn("Authorization header is missing or empty.")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        try {
            val decryptedBytes = mipDecryptionService.decrypt(file.inputStream, token)
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_OCTET_STREAM
            headers.contentDisposition = org.springframework.http.ContentDisposition.builder("attachment")
                .filename(file.originalFilename ?: "decrypted-file")
                .build()

            logger.info("Successfully decrypted file: ${file.originalFilename}")
            return ResponseEntity(decryptedBytes, headers, HttpStatus.OK)
        } catch (e: Exception) {
            logger.error("Error decrypting file: ${file.originalFilename}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping("/is-protected")
    fun isFileProtected(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<Boolean> {
        if (file.isEmpty) {
            logger.warn("Received an empty file upload for protection check.")
            return ResponseEntity.badRequest().build()
        }

        try {
            val isProtected = mipDecryptionService.isProtected(file.inputStream)
            return ResponseEntity.ok(isProtected)
        } catch (e: Exception) {
            logger.error("Error checking protection status for file: ${file.originalFilename}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}