package com.rnr.aip.service

import com.rnr.aip.exception.DecryptionException
import com.rnr.aip.exception.InvalidMipTokenException
import com.rnr.aip.service.decryption.DecryptionService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path

@ExtendWith(MockitoExtension::class)
class DecryptionServiceTest {

    private lateinit var service: DecryptionService
    private val testClientId = "test-client-id"
    private val testAppName = "test-app"
    private val testAppVersion = "1.0"
    private val testToken = "test-bearer-token"

    @BeforeEach
    fun setUp() {
        service = spy(DecryptionService(testClientId, testAppName, testAppVersion))
    }

    @Nested
    @DisplayName("decrypt() method tests")
    inner class DecryptTests {

        @Test
        @DisplayName("Should successfully decrypt protected file")
        fun `when file is protected, then decrypt successfully`() {
            val testContent = "protected file content".toByteArray()
            val decryptedContent = "decrypted content".toByteArray()
            val inputStream = ByteArrayInputStream(testContent)

            doReturn(true).whenever(service).isFileProtected(any())
            doReturn(decryptedContent).whenever(service).performMipDecryption(any(), any())

            val result = service.decrypt(inputStream, testToken)

            assertArrayEquals(decryptedContent, result)
            verify(service).isFileProtected(any())
            verify(service).performMipDecryption(any(), eq(testToken))
        }

        @Test
        @DisplayName("Should throw exception when token is empty")
        fun `when token is empty, then throw InvalidMipTokenException`() {
            val inputStream = ByteArrayInputStream("test".toByteArray())

            val exception = assertThrows(InvalidMipTokenException::class.java) {
                service.decrypt(inputStream, "")
            }
            assertEquals("MIP token cannot be empty", exception.message)
        }

        @Test
        @DisplayName("Should throw exception when token is blank")
        fun `when token is blank, then throw InvalidMipTokenException`() {
            val inputStream = ByteArrayInputStream("test".toByteArray())

            val exception = assertThrows(InvalidMipTokenException::class.java) {
                service.decrypt(inputStream, "   ")
            }
            assertEquals("MIP token cannot be empty", exception.message)
        }

        @Test
        @DisplayName("Should throw exception when file is not protected")
        fun `when file is not protected, then throw DecryptionException`() {
            val inputStream = ByteArrayInputStream("unprotected file".toByteArray())
            doReturn(false).whenever(service).isFileProtected(any())

            val exception = assertThrows(DecryptionException::class.java) {
                service.decrypt(inputStream, testToken)
            }
            assertTrue(exception.message?.contains("File is not protected") == true)
        }

        @Test
        @DisplayName("Should handle MIP decryption failure")
        fun `when MIP decryption fails, then throw DecryptionException`() {
            val inputStream = ByteArrayInputStream("protected file".toByteArray())
            doReturn(true).whenever(service).isFileProtected(any())
            doThrow(DecryptionException("MIP SDK error")).whenever(service).performMipDecryption(any(), any())

            val exception = assertThrows(DecryptionException::class.java) {
                service.decrypt(inputStream, testToken)
            }
            assertTrue(exception.message?.contains("MIP SDK error") == true)
        }

        @Test
        @DisplayName("Should handle IOException gracefully")
        fun `when IOException occurs, then handle gracefully`() {
            val inputStream = mock<InputStream> {
                on { available() } doThrow (IOException("Stream error"))
            }

            val exception = assertThrows(Exception::class.java) {
                service.decrypt(inputStream, testToken)
            }
            assertTrue(exception.message?.contains("Failed to read input stream") == true)
        }
    }

    @Nested
    @DisplayName("isProtected() method tests")
    inner class IsProtectedTests {

        @Test
        @DisplayName("Should return true for protected file")
        fun `when file is protected, then return true`() {
            val inputStream = ByteArrayInputStream("protected content".toByteArray())
            doReturn(true).whenever(service).isFileProtected(any())

            val result = service.isProtected(inputStream)

            assertTrue(result)
            verify(service).isFileProtected(any())
        }

        @Test
        @DisplayName("Should return false for unprotected file")
        fun `when file is unprotected, then return false`() {
            val inputStream = ByteArrayInputStream("unprotected content".toByteArray())
            doReturn(false).whenever(service).isFileProtected(any())

            val result = service.isProtected(inputStream)

            assertFalse(result)
            verify(service).isFileProtected(any())
        }

        @Test
        @DisplayName("Should handle exception during protection check")
        fun `when exception occurs during protection check, then handle gracefully`() {
            val inputStream = mock<InputStream> {
                on { available() } doThrow (IOException("Stream error"))
            }

            val exception = assertThrows(Exception::class.java) {
                service.isProtected(inputStream)
            }
            assertTrue(exception.message?.contains("Failed to read input stream") == true)
        }

        @Test
        @DisplayName("Should reset markable input stream")
        fun `when input stream is markable, then reset after check`() {
            val inputStream = mock<InputStream> {
                on { markSupported() } doReturn true
                on { available() } doReturn 1
                on { read(any()) } doReturn -1
            }

            doReturn(false).whenever(service).isFileProtected(any())

            service.isProtected(inputStream)

            verify(inputStream).mark(Int.MAX_VALUE)
            verify(inputStream).reset()
        }

        @Test
        @DisplayName("Should handle non-markable input stream")
        fun `when input stream is non-markable, then skip mark and reset`() {
            val inputStream = mock<InputStream> {
                on { markSupported() } doReturn false
                on { available() } doReturn (1)
                on { read(any()) } doReturn -1
            }
            doReturn(false).whenever(service).isFileProtected(any())

            service.isProtected(inputStream)

            verify(inputStream, never()).mark(any())
            verify(inputStream, never()).reset()
        }
    }

    @Nested
    @DisplayName("isFileProtected() method tests")
    inner class IsFileProtectedTests {

        @TempDir
        lateinit var tempDir: Path

        @Test
        @DisplayName("Should return true for file with Microsoft and Protection markers")
        fun `when file has Microsoft and Protection markers, then return true`() {
            val testFile = tempDir.resolve("protected.txt").toFile()
            testFile.writeText("Microsoft file with Protection markers")

            val result = service.isFileProtected(testFile.absolutePath)
            assertTrue(result)
        }

        @Test
        @DisplayName("Should return false for unprotected file")
        fun `when file has no protection markers, then return false`() {
            val testFile = tempDir.resolve("unprotected.txt").toFile()
            testFile.writeText("Regular content")

            val result = service.isFileProtected(testFile.absolutePath)
            assertFalse(result)
        }

        @Test
        @DisplayName("Should handle non-existent file")
        fun `when file does not exist, then return false`() {
            val nonExistentFile = tempDir.resolve("no.txt").toFile().absolutePath
            val result = service.isFileProtected(nonExistentFile)
            assertFalse(result)
        }
    }
}
