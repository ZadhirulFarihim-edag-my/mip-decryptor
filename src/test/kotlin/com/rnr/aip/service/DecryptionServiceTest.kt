package com.rnr.aip.service

import com.rnr.aip.exception.DecryptionException
import com.rnr.aip.exception.EmptyFileException
import com.rnr.aip.exception.FileProtectionCheckException
import com.rnr.aip.exception.InvalidMipTokenException
import com.rnr.aip.service.decryption.DecryptionService
import com.rnr.aip.service.decryption.FileValidator
import com.rnr.aip.service.decryption.MipFileHandler
import com.rnr.aip.utils.FileProtectionChecker
import com.rnr.aip.utils.TempFileManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

@ExtendWith(MockitoExtension::class)
class DecryptionServiceTest {

    @Mock(lenient = true)
    private lateinit var fileValidator: FileValidator

    @Mock(lenient = true)
    private lateinit var tempFileManager: TempFileManager

    @Mock(lenient = true)
    private lateinit var fileProtectionChecker: FileProtectionChecker

    @Mock(lenient = true)
    private lateinit var mipFileHandler: MipFileHandler

    @InjectMocks
    private lateinit var cut: DecryptionService

    private val testToken = "test-bearer-token"
    private val testFilePath = "/tmp/test-file.txt"

    @Nested
    @DisplayName("decrypt() method tests")
    inner class DecryptTests {

        @Test
        @DisplayName("Should successfully decrypt protected file")
        fun `when file is protected, then decrypt successfully`() {
            val testContent = "protected file content".toByteArray()
            val decryptedContent = "decrypted content".toByteArray()
            val inputStream = ByteArrayInputStream(testContent)
            val tempFile = mock<File> {
                on { absolutePath } doReturn testFilePath
                on { delete() } doReturn true
            }

            whenever(tempFileManager.createTempFile(any(), eq("decrypt-"))).thenReturn(tempFile)
            whenever(fileProtectionChecker.isFileProtected(testFilePath)).thenReturn(true)
            whenever(mipFileHandler.decryptFile(testFilePath, testToken)).thenReturn(decryptedContent)

            val result = cut.decrypt(inputStream, testToken)

            assertArrayEquals(decryptedContent, result)
            verify(fileValidator).validateMipToken(testToken)
            verify(fileValidator).validateInputStream(inputStream)
            verify(tempFileManager).createTempFile(inputStream, "decrypt-")
            verify(fileProtectionChecker).isFileProtected(testFilePath)
            verify(mipFileHandler).decryptFile(testFilePath, testToken)
            verify(tempFile).delete()
        }

        @Test
        @DisplayName("Should throw exception when token is empty")
        fun `when token is empty, then throw InvalidMipTokenException`() {
            val inputStream = ByteArrayInputStream("test".toByteArray())
            val emptyToken = ""

            doThrow(InvalidMipTokenException("MIP token cannot be empty"))
                .whenever(fileValidator).validateMipToken(emptyToken)

            val exception = assertThrows(InvalidMipTokenException::class.java) {
                cut.decrypt(inputStream, emptyToken)
            }

            assertEquals("MIP token cannot be empty", exception.message)
            verify(fileValidator).validateMipToken(emptyToken)
            verifyNoInteractions(tempFileManager, fileProtectionChecker, mipFileHandler)
        }

        @Test
        @DisplayName("Should throw exception when token is blank")
        fun `when token is blank, then throw InvalidMipTokenException`() {
            val inputStream = ByteArrayInputStream("test".toByteArray())
            val blankToken = "   "

            doThrow(InvalidMipTokenException("MIP token cannot be empty"))
                .whenever(fileValidator).validateMipToken(blankToken)

            val exception = assertThrows(InvalidMipTokenException::class.java) {
                cut.decrypt(inputStream, blankToken)
            }

            assertEquals("MIP token cannot be empty", exception.message)
            verify(fileValidator).validateMipToken(blankToken)
        }

        @Test
        @DisplayName("Should throw exception when input stream is empty")
        fun `when input stream is empty, then throw EmptyFileException`() {
            val inputStream = ByteArrayInputStream(ByteArray(0))

            doThrow(EmptyFileException("File is empty"))
                .whenever(fileValidator).validateInputStream(inputStream)

            val exception = assertThrows(EmptyFileException::class.java) {
                cut.decrypt(inputStream, testToken)
            }

            assertEquals("File is empty", exception.message)
            verify(fileValidator).validateInputStream(inputStream)
        }

        @Test
        @DisplayName("Should throw exception when file is not protected")
        fun `when file is not protected, then throw DecryptionException`() {
            val inputStream = ByteArrayInputStream("unprotected file".toByteArray())
            val tempFile = mock<File> {
                on { absolutePath } doReturn testFilePath
                on { delete() } doReturn true
            }

            whenever(tempFileManager.createTempFile(any(), eq("decrypt-"))).thenReturn(tempFile)
            whenever(fileProtectionChecker.isFileProtected(testFilePath)).thenReturn(false)

            val exception = assertThrows(DecryptionException::class.java) {
                cut.decrypt(inputStream, testToken)
            }

            assertTrue(exception.message?.contains("File is not protected") == true)
            verify(tempFile).delete()
            verify(mipFileHandler, never()).decryptFile(any(), any())
        }

        @Test
        @DisplayName("Should handle MIP decryption failure")
        fun `when MIP decryption fails, then throw DecryptionException`() {
            val inputStream = ByteArrayInputStream("protected file".toByteArray())
            val tempFile = mock<File> {
                on { absolutePath } doReturn testFilePath
                on { delete() } doReturn true
            }

            whenever(tempFileManager.createTempFile(any(), eq("decrypt-"))).thenReturn(tempFile)
            whenever(fileProtectionChecker.isFileProtected(testFilePath)).thenReturn(true)
            whenever(mipFileHandler.decryptFile(testFilePath, testToken))
                .thenThrow(DecryptionException("MIP SDK error"))

            val exception = assertThrows(DecryptionException::class.java) {
                cut.decrypt(inputStream, testToken)
            }

            assertTrue(exception.message?.contains("MIP SDK error") == true)
            verify(tempFile).delete()
        }

        @Test
        @DisplayName("Should clean up temp file even when exception occurs")
        fun `when exception occurs, then cleanup temp file`() {
            val inputStream = ByteArrayInputStream("test content".toByteArray())
            val tempFile = mock<File> {
                on { absolutePath } doReturn testFilePath
                on { delete() } doReturn true
            }

            whenever(tempFileManager.createTempFile(any(), eq("decrypt-"))).thenReturn(tempFile)
            whenever(fileProtectionChecker.isFileProtected(testFilePath))
                .thenThrow(RuntimeException("Unexpected error"))

            assertThrows(Exception::class.java) {
                cut.decrypt(inputStream, testToken)
            }

            verify(tempFile).delete()
        }
    }

    @Nested
    @DisplayName("isProtected() method tests")
    inner class IsProtectedTests {

        @Test
        @DisplayName("Should return true for protected file")
        fun `when file is protected, then return true`() {
            val inputStream = ByteArrayInputStream("protected content".toByteArray())
            val tempFile = mock<File> {
                on { absolutePath } doReturn testFilePath
                on { delete() } doReturn true
            }

            whenever(tempFileManager.createTempFile(any(), eq("isprotected-"))).thenReturn(tempFile)
            whenever(fileProtectionChecker.isFileProtected(testFilePath)).thenReturn(true)

            val result = cut.isProtected(inputStream)

            assertTrue(result)
            verify(fileValidator).validateInputStream(inputStream)
            verify(tempFileManager).createTempFile(any(), eq("isprotected-"))
            verify(fileProtectionChecker).isFileProtected(testFilePath)
            verify(tempFile).delete()
        }

        @Test
        @DisplayName("Should return false for unprotected file")
        fun `when file is unprotected, then return false`() {
            val inputStream = ByteArrayInputStream("unprotected content".toByteArray())
            val tempFile = mock<File> {
                on { absolutePath } doReturn testFilePath
                on { delete() } doReturn true
            }

            whenever(tempFileManager.createTempFile(any(), eq("isprotected-"))).thenReturn(tempFile)
            whenever(fileProtectionChecker.isFileProtected(testFilePath)).thenReturn(false)

            val result = cut.isProtected(inputStream)

            assertFalse(result)
            verify(fileProtectionChecker).isFileProtected(testFilePath)
            verify(tempFile).delete()
        }

        @Test
        @DisplayName("Should handle exception during validation")
        fun `when validation fails, then throw exception`() {
            val inputStream = ByteArrayInputStream("test".toByteArray())

            doThrow(EmptyFileException("File is empty"))
                .whenever(fileValidator).validateInputStream(inputStream)

            val exception = assertThrows(EmptyFileException::class.java) {
                cut.isProtected(inputStream)
            }

            assertEquals("File is empty", exception.message)
            verifyNoInteractions(tempFileManager, fileProtectionChecker)
        }

        @Test
        @DisplayName("Should handle exception during protection check")
        fun `when protection check fails, then throw FileProtectionCheckException`() {
            val inputStream = ByteArrayInputStream("test".toByteArray())
            val tempFile = mock<File> {
                on { absolutePath } doReturn testFilePath
                on { delete() } doReturn true
            }

            whenever(tempFileManager.createTempFile(any(), eq("isprotected-"))).thenReturn(tempFile)
            whenever(fileProtectionChecker.isFileProtected(testFilePath))
                .thenThrow(RuntimeException("Check failed"))

            val exception = assertThrows(FileProtectionCheckException::class.java) {
                cut.isProtected(inputStream)
            }

            assertTrue(exception.message?.contains("Failed to check file protection status") == true)
            verify(tempFile).delete()
        }

        @Test
        @DisplayName("Should reset markable input stream")
        fun `when input stream is markable, then reset after check`() {
            val inputStream = mock<InputStream> {
                on { markSupported() } doReturn true
                on { available() } doReturn 1
                on { read(any()) } doReturn -1
            }
            val tempFile = mock<File> {
                on { absolutePath } doReturn testFilePath
                on { delete() } doReturn true
            }

            whenever(tempFileManager.createTempFile(any(), eq("isprotected-"))).thenReturn(tempFile)
            whenever(fileProtectionChecker.isFileProtected(testFilePath)).thenReturn(false)

            cut.isProtected(inputStream)

            verify(inputStream).mark(Int.MAX_VALUE)
            verify(inputStream).reset()
        }

        @Test
        @DisplayName("Should handle non-markable input stream")
        fun `when input stream is non-markable, then skip mark and reset`() {
            val inputStream = mock<InputStream> {
                on { markSupported() } doReturn false
                on { available() } doReturn 1
                on { read(any()) } doReturn -1
            }
            val tempFile = mock<File> {
                on { absolutePath } doReturn testFilePath
                on { delete() } doReturn true
            }

            whenever(tempFileManager.createTempFile(any(), eq("isprotected-"))).thenReturn(tempFile)
            whenever(fileProtectionChecker.isFileProtected(testFilePath)).thenReturn(false)

            cut.isProtected(inputStream)

            verify(inputStream, never()).mark(any())
            verify(inputStream, never()).reset()
        }

        @Test
        @DisplayName("Should clean up temp file even when exception occurs")
        fun `when exception occurs, then cleanup temp file`() {
            val inputStream = ByteArrayInputStream("test".toByteArray())
            val tempFile = mock<File> {
                on { absolutePath } doReturn testFilePath
                on { delete() } doReturn true
            }

            whenever(tempFileManager.createTempFile(any(), eq("isprotected-"))).thenReturn(tempFile)
            whenever(fileProtectionChecker.isFileProtected(testFilePath))
                .thenThrow(RuntimeException("Test error"))

            assertThrows(FileProtectionCheckException::class.java) {
                cut.isProtected(inputStream)
            }

            verify(tempFile).delete()
        }
    }
}