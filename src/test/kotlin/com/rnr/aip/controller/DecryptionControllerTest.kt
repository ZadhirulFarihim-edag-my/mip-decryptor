package com.rnr.aip.controller

import com.rnr.aip.exception.DecryptionException
import com.rnr.aip.exception.InvalidMipTokenException
import com.rnr.aip.service.decryption.DecryptionService
import com.rnr.aip.utils.WebIntegrationTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.io.InputStream

@WebIntegrationTest
class DecryptionControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @Autowired
    @MockitoBean
    private lateinit var decryptionService: DecryptionService

    private val testToken =
        "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0LXVzZXIiLCJvaWQiOiJ0ZXN0LW9pZCIsImF6cCI6InRlc3QtY2xpZW50LWlkIiwiZXhwIjoxOTk5OTk5OTk5fQ.test-signature"

    @Nested
    @DisplayName("POST /api/decrypt - Decrypt file endpoint tests")
    inner class DecryptEndpointTests {

        @Test
        @DisplayName("Should successfully decrypt protected file with valid token")
        fun `when valid file and token provided, then return decrypted file`() {
            val originalFilename = "protected-document.docx"
            val fileContent = "protected content".toByteArray()
            val decryptedContent = "decrypted content".toByteArray()

            val multipartFile = MockMultipartFile(
                "file",
                originalFilename,
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                fileContent
            )

            whenever(decryptionService.decrypt(any<InputStream>(), eq(testToken)))
                .thenReturn(decryptedContent)

            mockMvc.perform(
                multipart("/api/decrypt")
                    .file(multipartFile)
                    .header("rnr-mip-token", testToken)
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"$originalFilename\""))
                .andExpect(content().bytes(decryptedContent))

            verify(decryptionService).decrypt(any<InputStream>(), eq(testToken))
        }

        @Test
        @DisplayName("Should return 400 when file parameter is missing")
        fun `when file parameter is missing, then return bad request`() {
            mockMvc.perform(
                multipart("/api/decrypt")
                    .header("rnr-mip-token", testToken)
            )
                .andExpect(status().isBadRequest)

            verify(decryptionService, never()).decrypt(any<InputStream>(), any())
        }

        @Test
        @DisplayName("Should return 401 when mip token header is missing")
        fun `when mip token header is missing, then return unauthorized`() {
            val multipartFile = MockMultipartFile(
                "file",
                "test.docx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "content".toByteArray()
            )

            mockMvc.perform(
                multipart("/api/decrypt")
                    .file(multipartFile)
            )
                .andExpect(status().isUnauthorized)

            verify(decryptionService, never()).decrypt(any<InputStream>(), any())
        }

        @Test
        @DisplayName("Should return 401 when token is invalid and service throws exception")
        fun `when token is invalid, then return unauthorized with error message`() {
            val invalidToken = "invalid-token"
            val multipartFile = MockMultipartFile(
                "file",
                "test.docx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "content".toByteArray()
            )

            whenever(decryptionService.decrypt(any<InputStream>(), eq(invalidToken)))
                .thenThrow(InvalidMipTokenException("Invalid MIP token format"))

            mockMvc.perform(
                multipart("/api/decrypt")
                    .file(multipartFile)
                    .header("rnr-mip-token", invalidToken)
            )
                .andExpect(status().isUnauthorized)

            verify(decryptionService).decrypt(any<InputStream>(), eq(invalidToken))
        }

        @Test
        @DisplayName("Should return 401 when token is empty and service throws exception")
        fun `when token is empty, then return unauthorized with error message`() {
            val emptyToken = ""
            val multipartFile = MockMultipartFile(
                "file",
                "test.docx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "content".toByteArray()
            )

            whenever(decryptionService.decrypt(any<InputStream>(), eq(emptyToken)))
                .thenThrow(InvalidMipTokenException("MIP token cannot be empty"))

            mockMvc.perform(
                multipart("/api/decrypt")
                    .file(multipartFile)
                    .header("rnr-mip-token", emptyToken)
            )
                .andExpect(status().isUnauthorized)

            verify(decryptionService).decrypt(any<InputStream>(), eq(emptyToken))
        }

        @Test
        @DisplayName("Should return 500 when decryption service throws DecryptionException")
        fun `when decryption fails, then return internal server error`() {
            val multipartFile = MockMultipartFile(
                "file",
                "protected.docx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "protected content".toByteArray()
            )

            whenever(decryptionService.decrypt(any<InputStream>(), eq(testToken)))
                .thenThrow(DecryptionException("MIP SDK decryption failed"))

            mockMvc.perform(
                multipart("/api/decrypt")
                    .file(multipartFile)
                    .header("rnr-mip-token", testToken)
            )
                .andExpect(status().isInternalServerError)

            verify(decryptionService).decrypt(any<InputStream>(), eq(testToken))
        }

        @Test
        @DisplayName("Should handle file with no original filename")
        fun `when file has no original filename, then use empty filename`() {
            val decryptedContent = "decrypted content".toByteArray()
            val multipartFile = MockMultipartFile(
                "file",
                null,
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "content".toByteArray()
            )

            whenever(decryptionService.decrypt(any<InputStream>(), eq(testToken)))
                .thenReturn(decryptedContent)

            mockMvc.perform(
                multipart("/api/decrypt")
                    .file(multipartFile)
                    .header("rnr-mip-token", testToken)
            )
                .andExpect(status().isOk)
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"\""))
                .andExpect(content().bytes(decryptedContent))

            verify(decryptionService).decrypt(any<InputStream>(), eq(testToken))
        }

        @Test
        @DisplayName("Should successfully decrypt file with Azure AD token format")
        fun `when Azure AD token provided, then decrypt successfully`() {
            val azureAdToken =
                "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJhcGk6Ly90ZXN0LWNsaWVudC1pZCIsImlzcyI6Imh0dHBzOi8vbG9naW4ubWljcm9zb2Z0b25saW5lLmNvbS90ZXN0LXRlbmFudC1pZC92Mi4wIiwiaWF0IjoxNjAwMDAwMDAwLCJuYmYiOjE2MDAwMDAwMDAsImV4cCI6MTYwMDAwMzYwMCwib2lkIjoidGVzdC11c2VyLW9pZCIsInN1YiI6InRlc3Qtc3ViamVjdCIsInRpZCI6InRlc3QtdGVuYW50LWlkIiwidXBuIjoidGVzdEBleGFtcGxlLmNvbSIsInZlciI6IjIuMCJ9.mock-signature"
            val fileContent = "protected content".toByteArray()
            val decryptedContent = "decrypted content from Azure AD".toByteArray()

            val multipartFile = MockMultipartFile(
                "file",
                "azure-protected.xlsx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                fileContent
            )

            whenever(decryptionService.decrypt(any<InputStream>(), eq(azureAdToken)))
                .thenReturn(decryptedContent)

            mockMvc.perform(
                multipart("/api/decrypt")
                    .file(multipartFile)
                    .header("rnr-mip-token", azureAdToken)
            )
                .andExpect(status().isOk)
                .andExpect(content().bytes(decryptedContent))

            verify(decryptionService).decrypt(any<InputStream>(), eq(azureAdToken))
        }
    }

    @Nested
    @DisplayName("POST /api/is-protected - Check file protection endpoint tests")
    inner class IsProtectedEndpointTests {

        @Test
        @DisplayName("Should return true when file is protected")
        fun `when file is protected, then return true`() {
            val multipartFile = MockMultipartFile(
                "file",
                "protected.docx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "protected content".toByteArray()
            )

            whenever(decryptionService.isProtected(any<InputStream>()))
                .thenReturn(true)

            mockMvc.perform(
                multipart("/api/is-protected")
                    .file(multipartFile)
            )
                .andExpect(status().isOk)
                .andExpect(content().string("true"))

            verify(decryptionService).isProtected(any<InputStream>())
        }

        @Test
        @DisplayName("Should return false when file is not protected")
        fun `when file is not protected, then return false`() {
            val multipartFile = MockMultipartFile(
                "file",
                "unprotected.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "plain text content".toByteArray()
            )

            whenever(decryptionService.isProtected(any<InputStream>()))
                .thenReturn(false)

            mockMvc.perform(
                multipart("/api/is-protected")
                    .file(multipartFile)
            )
                .andExpect(status().isOk)
                .andExpect(content().string("false"))

            verify(decryptionService).isProtected(any<InputStream>())
        }

        @Test
        @DisplayName("Should return 400 when file parameter is missing")
        fun `when file parameter is missing, then return bad request`() {
            mockMvc.perform(
                multipart("/api/is-protected")
            )
                .andExpect(status().isBadRequest)

            verify(decryptionService, never()).isProtected(any<InputStream>())
        }

        @Test
        @DisplayName("Should check protection without requiring authentication token")
        fun `when checking file protection, then no token required`() {
            val multipartFile = MockMultipartFile(
                "file",
                "test.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf content".toByteArray()
            )

            whenever(decryptionService.isProtected(any<InputStream>()))
                .thenReturn(true)

            mockMvc.perform(
                multipart("/api/is-protected")
                    .file(multipartFile)
            )
                .andExpect(status().isOk)
                .andExpect(content().string("true"))

            verify(decryptionService).isProtected(any<InputStream>())
        }

        @Test
        @DisplayName("Should handle various file types")
        fun `when checking different file types, then return protection status`() {
            // Test for PDF
            val pdfFile = MockMultipartFile(
                "file",
                "document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf content".toByteArray()
            )

            whenever(decryptionService.isProtected(any<InputStream>()))
                .thenReturn(true)

            mockMvc.perform(
                multipart("/api/is-protected")
                    .file(pdfFile)
            )
                .andExpect(status().isOk)
                .andExpect(content().string("true"))

            // Test for DOCX
            val docxFile = MockMultipartFile(
                "file",
                "document.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "docx content".toByteArray()
            )

            whenever(decryptionService.isProtected(any<InputStream>()))
                .thenReturn(false)

            mockMvc.perform(
                multipart("/api/is-protected")
                    .file(docxFile)
            )
                .andExpect(status().isOk)
                .andExpect(content().string("false"))

            verify(decryptionService, times(2)).isProtected(any<InputStream>())
        }
    }
}