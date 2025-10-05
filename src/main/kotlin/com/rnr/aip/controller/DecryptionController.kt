package com.rnr.aip.controller

import com.rnr.aip.service.decryption.DecryptionService
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api")
class DecryptionController(
    private val decryptionService: DecryptionService
) {

    @PostMapping("/decrypt")
    @ResponseStatus(HttpStatus.OK)
    fun decryptFile(
        @RequestParam("file") file: MultipartFile,
        @RequestHeader("rnr-mip-token") mipToken: String
    ): ResponseEntity<ByteArray> {
        val decryptedBytes = decryptionService.decrypt(file.inputStream, mipToken)

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_OCTET_STREAM
            contentDisposition = ContentDisposition.builder("attachment")
                .filename(file.originalFilename ?: "decrypted-file")
                .build()
        }

        return ResponseEntity.ok().headers(headers).body(decryptedBytes)
    }

    @PostMapping("/is-protected")
    @ResponseStatus(HttpStatus.OK)
    fun isFileProtected(
        @RequestParam("file") file: MultipartFile
    ): Boolean {
        val isProtected = decryptionService.isProtected(file.inputStream)
        return isProtected
    }
}